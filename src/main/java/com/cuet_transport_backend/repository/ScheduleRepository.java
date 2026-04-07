package com.cuet_transport_backend.repository;

import com.cuet_transport_backend.model.Bus;
import com.cuet_transport_backend.model.Driver;
import com.cuet_transport_backend.model.Route;
import com.cuet_transport_backend.model.Schedule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByBus(Bus bus);

    List<Schedule> findByRoute(Route route);

    List<Schedule> findByDriver(Driver driver);
}
