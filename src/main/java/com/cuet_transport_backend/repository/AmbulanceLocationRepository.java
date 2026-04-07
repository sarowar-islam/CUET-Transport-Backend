package com.cuet_transport_backend.repository;

import com.cuet_transport_backend.model.Ambulance;
import com.cuet_transport_backend.model.AmbulanceLocation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AmbulanceLocationRepository extends JpaRepository<AmbulanceLocation, Long> {
    Optional<AmbulanceLocation> findTopByAmbulanceOrderByTimestampDesc(Ambulance ambulance);

    List<AmbulanceLocation> findByAmbulanceOrderByTimestampDesc(Ambulance ambulance);
}
