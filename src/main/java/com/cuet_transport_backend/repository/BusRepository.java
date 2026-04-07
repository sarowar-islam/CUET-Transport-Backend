package com.cuet_transport_backend.repository;

import com.cuet_transport_backend.model.Bus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusRepository extends JpaRepository<Bus, Long> {
    List<Bus> findByNameContainingIgnoreCase(String name);
}
