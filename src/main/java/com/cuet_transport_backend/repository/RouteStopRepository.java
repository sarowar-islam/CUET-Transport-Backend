package com.cuet_transport_backend.repository;

import com.cuet_transport_backend.model.Route;
import com.cuet_transport_backend.model.RouteStop;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {
    List<RouteStop> findByRouteOrderByStopOrderAsc(Route route);

    void deleteByRoute(Route route);
}
