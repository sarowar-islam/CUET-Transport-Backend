package com.cuet_transport_backend.repository;

import com.cuet_transport_backend.model.Driver;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    List<Driver> findByNameContainingIgnoreCase(String name);
}
