package com.cuet_transport_backend.repository;

import com.cuet_transport_backend.model.Bus;
import com.cuet_transport_backend.model.Driver;
import com.cuet_transport_backend.model.Route;
import com.cuet_transport_backend.model.Schedule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    @Query("""
            select s from Schedule s
            join fetch s.bus
            join fetch s.route
            join fetch s.driver
            """)
    List<Schedule> findAllWithRelations();

    @Query("""
            select s from Schedule s
            join fetch s.bus
            join fetch s.route
            join fetch s.driver
            where s.id = :id
            """)
    Optional<Schedule> findByIdWithRelations(@Param("id") Long id);

    @Query("""
            select s from Schedule s
            join fetch s.bus
            join fetch s.route
            join fetch s.driver
            where s.bus = :bus
            """)
    List<Schedule> findByBusWithRelations(@Param("bus") Bus bus);

    @Query("""
            select s from Schedule s
            join fetch s.bus
            join fetch s.route
            join fetch s.driver
            where s.route = :route
            """)
    List<Schedule> findByRouteWithRelations(@Param("route") Route route);

    @Query("""
            select s from Schedule s
            join fetch s.bus
            join fetch s.route
            join fetch s.driver
            where s.driver = :driver
            """)
    List<Schedule> findByDriverWithRelations(@Param("driver") Driver driver);

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
