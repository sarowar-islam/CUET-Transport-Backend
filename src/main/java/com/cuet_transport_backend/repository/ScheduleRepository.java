package com.cuet_transport_backend.repository;

import com.cuet_transport_backend.model.Bus;
import com.cuet_transport_backend.model.Driver;
import com.cuet_transport_backend.model.Route;
import com.cuet_transport_backend.model.Schedule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByBus(Bus bus);

    List<Schedule> findByRoute(Route route);

    List<Schedule> findByDriver(Driver driver);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Schedule s where s.bus.id = :busId")
    int deleteByBusId(@Param("busId") Long busId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Schedule s where s.route.id = :routeId")
    int deleteByRouteId(@Param("routeId") Long routeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Schedule s where s.driver.id = :driverId")
    int deleteByDriverId(@Param("driverId") Long driverId);
}
