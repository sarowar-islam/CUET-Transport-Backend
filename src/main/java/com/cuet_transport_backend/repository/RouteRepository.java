package com.cuet_transport_backend.repository;

import com.cuet_transport_backend.model.Route;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findByNameContainingIgnoreCase(String name);
}
