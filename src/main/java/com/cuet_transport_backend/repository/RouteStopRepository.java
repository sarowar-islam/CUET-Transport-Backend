package com.cuet_transport_backend.repository;

import com.cuet_transport_backend.model.Route;
import com.cuet_transport_backend.model.RouteStop;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {
    List<RouteStop> findByRouteOrderByStopOrderAsc(Route route);

    @Query("select rs from RouteStop rs join fetch rs.stop where rs.route = :route order by rs.stopOrder asc")
    List<RouteStop> findByRouteWithStopOrderByStopOrderAsc(@Param("route") Route route);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RouteStop rs where rs.route.id = :routeId")
    int deleteByRouteId(@Param("routeId") Long routeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RouteStop rs where rs.stop.id = :stopId")
    int deleteByStopId(@Param("stopId") Long stopId);
}
