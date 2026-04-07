package com.cuet_transport_backend.repository;

import com.cuet_transport_backend.model.AmbulanceRequest;
import com.cuet_transport_backend.model.User;
import com.cuet_transport_backend.model.enums.AmbulanceRequestStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AmbulanceRequestRepository extends JpaRepository<AmbulanceRequest, Long> {
    List<AmbulanceRequest> findByRequester(User requester);

    List<AmbulanceRequest> findByStatus(AmbulanceRequestStatus status);
}
