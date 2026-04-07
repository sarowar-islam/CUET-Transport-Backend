package com.cuet_transport_backend.repository;

import com.cuet_transport_backend.model.BusRequest;
import com.cuet_transport_backend.model.User;
import com.cuet_transport_backend.model.enums.BusRequestStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusRequestRepository extends JpaRepository<BusRequest, Long> {
    List<BusRequest> findByRequester(User requester);

    List<BusRequest> findByStatus(BusRequestStatus status);
}
