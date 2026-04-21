package com.cuet_transport_backend.repository;

import com.cuet_transport_backend.model.AmbulanceRequest;
import com.cuet_transport_backend.model.User;
import com.cuet_transport_backend.model.enums.AmbulanceRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AmbulanceRequestRepository extends JpaRepository<AmbulanceRequest, Long> {
    @Query("select ar from AmbulanceRequest ar join fetch ar.requester left join fetch ar.ambulance")
    List<AmbulanceRequest> findAllWithRequesterAndAmbulance();

    @Query("select ar from AmbulanceRequest ar join fetch ar.requester left join fetch ar.ambulance where ar.requester = :requester")
    List<AmbulanceRequest> findByRequesterWithRequesterAndAmbulance(@Param("requester") User requester);

    @Query("select ar from AmbulanceRequest ar join fetch ar.requester left join fetch ar.ambulance where ar.status = :status")
    List<AmbulanceRequest> findByStatusWithRequesterAndAmbulance(@Param("status") AmbulanceRequestStatus status);

    @Query("select ar from AmbulanceRequest ar join fetch ar.requester left join fetch ar.ambulance where ar.id = :id")
    Optional<AmbulanceRequest> findByIdWithRequesterAndAmbulance(@Param("id") Long id);
}
