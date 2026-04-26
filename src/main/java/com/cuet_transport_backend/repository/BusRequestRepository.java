package com.cuet_transport_backend.repository;

import com.cuet_transport_backend.model.BusRequest;
import com.cuet_transport_backend.model.User;
import com.cuet_transport_backend.model.enums.BusRequestStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusRequestRepository extends JpaRepository<BusRequest, Long> {
    List<BusRequest> findByRequester(User requester);

    List<BusRequest> findByStatus(BusRequestStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update BusRequest br set br.assignedBus = null where br.assignedBus.id = :busId")
    int clearBusAssignments(@Param("busId") Long busId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update BusRequest br set br.assignedDriver = null where br.assignedDriver.id = :driverId")
    int clearDriverAssignments(@Param("driverId") Long driverId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from BusRequest br where br.requester.id = :requesterId")
    int deleteByRequesterId(@Param("requesterId") Long requesterId);
}
