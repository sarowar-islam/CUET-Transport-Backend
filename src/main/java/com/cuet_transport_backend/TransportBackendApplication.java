package com.cuet_transport_backend;

import com.cuet_transport_backend.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class TransportBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TransportBackendApplication.class, args);
	}

}
