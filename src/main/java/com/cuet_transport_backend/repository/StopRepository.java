package com.cuet_transport_backend.repository;

import com.cuet_transport_backend.model.Stop;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StopRepository extends JpaRepository<Stop, Long> {
    List<Stop> findByNameContainingIgnoreCase(String name);
}
