package com.cuet_transport_backend.repository;

import com.cuet_transport_backend.model.Ambulance;
import com.cuet_transport_backend.model.enums.AmbulanceStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AmbulanceRepository extends JpaRepository<Ambulance, Long> {
    List<Ambulance> findByStatus(AmbulanceStatus status);

    Optional<Ambulance> findByVehicleNumberIgnoreCase(String vehicleNumber);
}
