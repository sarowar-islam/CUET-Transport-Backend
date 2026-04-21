package com.cuet_transport_backend.config;

import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class DatabaseUrlConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseUrlConfig.class);

    @Bean
    public DataSource dataSource(
            @Value("${app.database.url:}") String appDatabaseUrl,
            @Value("${spring.datasource.url:}") String springDatasourceUrl,
            @Value("${app.database.username:}") String appDatabaseUsername,
            @Value("${app.database.password:}") String appDatabasePassword,
            @Value("${spring.datasource.hikari.maximum-pool-size:10}") int maximumPoolSize,
            @Value("${spring.datasource.hikari.minimum-idle:2}") int minimumIdle,
            @Value("${spring.datasource.hikari.connection-timeout:10000}") long connectionTimeout,
            @Value("${spring.datasource.hikari.validation-timeout:3000}") long validationTimeout,
            @Value("${spring.datasource.hikari.idle-timeout:300000}") long idleTimeout,
            @Value("${spring.datasource.hikari.max-lifetime:600000}") long maxLifetime,
            @Value("${spring.datasource.hikari.keepalive-time:30000}") long keepaliveTime) {
        String rawUrl = StringUtils.hasText(appDatabaseUrl) ? appDatabaseUrl : springDatasourceUrl;
        rawUrl = rawUrl == null ? "" : rawUrl.trim();
        if ((rawUrl.startsWith("\"") && rawUrl.endsWith("\""))
                || (rawUrl.startsWith("'") && rawUrl.endsWith("'"))) {
            rawUrl = rawUrl.substring(1, rawUrl.length() - 1);
        }

        if (!StringUtils.hasText(rawUrl)) {
            log.warn("DATABASE_URL not provided. Starting with local H2 fallback datasource.");
            return createH2Fallback();
        }

        if (rawUrl.startsWith("jdbc:")) {
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(rawUrl);
            if (rawUrl.startsWith("jdbc:postgresql:")) {
                ds.setDriverClassName("org.postgresql.Driver");
            } else if (rawUrl.startsWith("jdbc:h2:")) {
                ds.setDriverClassName("org.h2.Driver");
            }
            applyPoolTuning(ds, maximumPoolSize, minimumIdle, connectionTimeout, validationTimeout,
                    idleTimeout, maxLifetime, keepaliveTime);
            if (StringUtils.hasText(appDatabaseUsername)) {
                ds.setUsername(appDatabaseUsername);
            }
            if (StringUtils.hasText(appDatabasePassword)) {
                ds.setPassword(appDatabasePassword);
            }
            if (rawUrl.startsWith("jdbc:h2:")) {
                log.info("Using configured JDBC datasource: {}", rawUrl);
                return ds;
            }
            return validateOrFallback(ds);
        }

        try {
            ParsedDb parsed = parsePostgresUrl(rawUrl);
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(parsed.jdbcUrl());
            ds.setDriverClassName("org.postgresql.Driver");
            applyPoolTuning(ds, maximumPoolSize, minimumIdle, connectionTimeout, validationTimeout,
                    idleTimeout, maxLifetime, keepaliveTime);
            ds.setUsername(StringUtils.hasText(appDatabaseUsername) ? appDatabaseUsername : parsed.username());
            ds.setPassword(StringUtils.hasText(appDatabasePassword) ? appDatabasePassword : parsed.password());
            return validateOrFallback(ds);
        } catch (RuntimeException ex) {
            log.warn("Failed to parse DATABASE_URL. Falling back to local H2 datasource. Reason: {}", ex.getMessage());
            return createH2Fallback();
        }
    }

    private void applyPoolTuning(
            HikariDataSource ds,
            int maximumPoolSize,
            int minimumIdle,
            long connectionTimeout,
            long validationTimeout,
            long idleTimeout,
            long maxLifetime,
            long keepaliveTime) {
        ds.setMaximumPoolSize(maximumPoolSize);
        ds.setMinimumIdle(minimumIdle);
        ds.setConnectionTimeout(connectionTimeout);
        ds.setValidationTimeout(validationTimeout);
        ds.setIdleTimeout(idleTimeout);
        ds.setMaxLifetime(maxLifetime);
        ds.setKeepaliveTime(keepaliveTime);
    }

    private DataSource validateOrFallback(HikariDataSource ds) {
        try (Connection ignored = ds.getConnection()) {
            log.info("Using PostgreSQL datasource: {}", ds.getJdbcUrl());
            return ds;
        } catch (SQLException ex) {
            log.warn("PostgreSQL connection failed. Falling back to local H2 datasource. Reason: {}", ex.getMessage());
            return createH2Fallback();
        }
    }

    private DataSource createH2Fallback() {
        HikariDataSource fallback = new HikariDataSource();
        fallback.setJdbcUrl("jdbc:h2:mem:transport;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        fallback.setDriverClassName("org.h2.Driver");
        fallback.setUsername("sa");
        fallback.setPassword("");
        return fallback;
    }

    private ParsedDb parsePostgresUrl(String url) {
        try {
            URI dbUri = new URI(url);
            String userInfo = dbUri.getUserInfo();
            String username = "";
            String password = "";
            if (userInfo != null && userInfo.contains(":")) {
                String[] userInfoParts = userInfo.split(":", 2);
                username = userInfoParts[0];
                password = userInfoParts[1];
            }

            String scheme = dbUri.getScheme();
            if (!"postgres".equalsIgnoreCase(scheme) && !"postgresql".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException("Unsupported DATABASE_URL scheme: " + scheme);
            }

            int port = dbUri.getPort() == -1 ? 5432 : dbUri.getPort();
            String query = dbUri.getRawQuery();
            String jdbcUrl = "jdbc:postgresql://" + dbUri.getHost() + ":" + port + dbUri.getPath();
            if (StringUtils.hasText(query)) {
                jdbcUrl = jdbcUrl + "?" + query;
            }

            return new ParsedDb(jdbcUrl, username, password);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid DATABASE_URL format", e);
        }
    }

    private record ParsedDb(String jdbcUrl, String username, String password) {
    }
}
