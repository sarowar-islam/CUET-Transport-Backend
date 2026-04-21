package com.cuet_transport_backend.repository;

import com.cuet_transport_backend.model.Ambulance;
import com.cuet_transport_backend.model.AmbulanceLocation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AmbulanceLocationRepository extends JpaRepository<AmbulanceLocation, Long> {
    Optional<AmbulanceLocation> findTopByAmbulanceOrderByTimestampDesc(Ambulance ambulance);

    List<AmbulanceLocation> findByAmbulanceOrderByTimestampDesc(Ambulance ambulance);

    void deleteByAmbulanceId(Long ambulanceId);

    @Query("""
            select l from AmbulanceLocation l
            where l.ambulance.id in :ambulanceIds
                and l.timestamp = (
                    select max(l2.timestamp)
                    from AmbulanceLocation l2
                    where l2.ambulance.id = l.ambulance.id
                )
            """)
    List<AmbulanceLocation> findLatestByAmbulanceIds(@Param("ambulanceIds") List<Long> ambulanceIds);
}
