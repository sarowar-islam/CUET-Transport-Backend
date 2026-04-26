package com.cuet_transport_backend.controller;

import com.cuet_transport_backend.model.Bus;
import com.cuet_transport_backend.model.Driver;
import com.cuet_transport_backend.model.Route;
import com.cuet_transport_backend.model.RouteStop;
import com.cuet_transport_backend.model.Schedule;
import com.cuet_transport_backend.model.Stop;
import com.cuet_transport_backend.model.User;
import com.cuet_transport_backend.model.enums.Direction;
import com.cuet_transport_backend.model.enums.UserRole;
import com.cuet_transport_backend.repository.BusRepository;
import com.cuet_transport_backend.repository.BusRequestRepository;
import com.cuet_transport_backend.repository.AmbulanceRequestRepository;
import com.cuet_transport_backend.repository.DriverRepository;
import com.cuet_transport_backend.repository.RouteRepository;
import com.cuet_transport_backend.repository.RouteStopRepository;
import com.cuet_transport_backend.repository.ScheduleRepository;
import com.cuet_transport_backend.repository.StopRepository;
import com.cuet_transport_backend.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CrudControllers {

    private final BusRepository busRepository;
    private final DriverRepository driverRepository;
    private final StopRepository stopRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final ScheduleRepository scheduleRepository;
    private final BusRequestRepository busRequestRepository;
    private final AmbulanceRequestRepository ambulanceRequestRepository;
    private final UserRepository userRepository;

    public record BusRequestDto(@NotBlank String name, @NotNull Integer capacity, @NotBlank String plateNumber) {
    }

    public record DriverRequestDto(@NotBlank String name, @NotBlank String phone, String photo) {
    }

    public record StopRequestDto(@NotBlank String name, @NotNull Double latitude, @NotNull Double longitude) {
    }

    public record RouteRequestDto(@NotBlank String name, @NotBlank String color, @NotNull List<Long> stopIds) {
    }

    public record ScheduleRequestDto(
            @NotNull Long busId,
            @NotNull Long routeId,
            @NotNull Long driverId,
            @NotBlank String departureTime,
            @NotNull Direction direction,
            @NotNull Set<UserRole> category) {
    }

    public record UserUpdateDto(String fullName, String email, UserRole role) {
    }

    // Buses
    @GetMapping("/buses")
    public List<Bus> allBuses() {
        return busRepository.findAll();
    }

    @GetMapping("/buses/{id}")
    public Bus busById(@PathVariable Long id) {
        return busRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Bus not found"));
    }

    @PostMapping("/buses")
    @PreAuthorize("hasRole('ADMIN')")
    public Bus createBus(@Valid @RequestBody BusRequestDto dto) {
        Bus bus = new Bus();
        bus.setName(dto.name());
        bus.setCapacity(dto.capacity());
        bus.setPlateNumber(dto.plateNumber());
        return busRepository.save(bus);
    }

    @PutMapping("/buses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Bus updateBus(@PathVariable Long id, @RequestBody BusRequestDto dto) {
        Bus bus = busById(id);
        bus.setName(dto.name());
        bus.setCapacity(dto.capacity());
        bus.setPlateNumber(dto.plateNumber());
        return busRepository.save(bus);
    }

    @DeleteMapping("/buses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> deleteBus(@PathVariable Long id) {
        scheduleRepository.deleteByBusId(id);
        busRequestRepository.clearBusAssignments(id);
        busRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buses/search")
    public List<Bus> searchBuses(@RequestParam String name) {
        return busRepository.findByNameContainingIgnoreCase(name);
    }

    // Drivers
    @GetMapping("/drivers")
    public List<Driver> allDrivers() {
        return driverRepository.findAll();
    }

    @GetMapping("/drivers/{id}")
    public Driver driverById(@PathVariable Long id) {
        return driverRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Driver not found"));
    }

    @PostMapping("/drivers")
    @PreAuthorize("hasRole('ADMIN')")
    public Driver createDriver(@Valid @RequestBody DriverRequestDto dto) {
        Driver driver = new Driver();
        driver.setName(dto.name());
        driver.setPhone(dto.phone());
        driver.setPhoto(dto.photo());
        return driverRepository.save(driver);
    }

    @PutMapping("/drivers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Driver updateDriver(@PathVariable Long id, @RequestBody DriverRequestDto dto) {
        Driver driver = driverById(id);
        driver.setName(dto.name());
        driver.setPhone(dto.phone());
        driver.setPhoto(dto.photo());
        return driverRepository.save(driver);
    }

    @DeleteMapping("/drivers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> deleteDriver(@PathVariable Long id) {
        scheduleRepository.deleteByDriverId(id);
        busRequestRepository.clearDriverAssignments(id);
        driverRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/drivers/search")
    public List<Driver> searchDrivers(@RequestParam String name) {
        return driverRepository.findByNameContainingIgnoreCase(name);
    }

    // Stops
    @GetMapping("/stops")
    public List<Stop> allStops() {
        return stopRepository.findAll();
    }

    @GetMapping("/stops/{id}")
    public Stop stopById(@PathVariable Long id) {
        return stopRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Stop not found"));
    }

    @PostMapping("/stops")
    @PreAuthorize("hasRole('ADMIN')")
    public Stop createStop(@Valid @RequestBody StopRequestDto dto) {
        Stop stop = new Stop();
        stop.setName(dto.name());
        stop.setLatitude(dto.latitude());
        stop.setLongitude(dto.longitude());
        return stopRepository.save(stop);
    }

    @PutMapping("/stops/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Stop updateStop(@PathVariable Long id, @RequestBody StopRequestDto dto) {
        Stop stop = stopById(id);
        stop.setName(dto.name());
        stop.setLatitude(dto.latitude());
        stop.setLongitude(dto.longitude());
        return stopRepository.save(stop);
    }

    @DeleteMapping("/stops/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> deleteStop(@PathVariable Long id) {
        routeStopRepository.deleteByStopId(id);
        stopRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stops/search")
    public List<Stop> searchStops(@RequestParam String name) {
        return stopRepository.findByNameContainingIgnoreCase(name);
    }

    // Routes
    public record StopResponse(Long id, String name, Double latitude, Double longitude) {
    }

    public record RouteResponse(Long id, String name, String color, List<StopResponse> stops) {
    }

    @GetMapping("/routes")
    public List<RouteResponse> allRoutes() {
        List<RouteResponse> response = new ArrayList<>();
        for (Route route : routeRepository.findAll()) {
            List<StopResponse> stops = routeStopRepository.findByRouteWithStopOrderByStopOrderAsc(route)
                    .stream()
                    .sorted(Comparator.comparing(RouteStop::getStopOrder))
                    .map(RouteStop::getStop)
                    .map(stop -> new StopResponse(stop.getId(), stop.getName(), stop.getLatitude(),
                            stop.getLongitude()))
                    .toList();
            response.add(new RouteResponse(route.getId(), route.getName(), route.getColor(), stops));
        }
        return response;
    }

    @GetMapping("/routes/{id}")
    public RouteResponse routeById(@PathVariable Long id) {
        Route route = routeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Route not found"));
        List<StopResponse> stops = routeStopRepository.findByRouteWithStopOrderByStopOrderAsc(route)
                .stream()
                .sorted(Comparator.comparing(RouteStop::getStopOrder))
                .map(RouteStop::getStop)
                .map(stop -> new StopResponse(stop.getId(), stop.getName(), stop.getLatitude(), stop.getLongitude()))
                .toList();
        return new RouteResponse(route.getId(), route.getName(), route.getColor(), stops);
    }

    @PostMapping("/routes")
    @PreAuthorize("hasRole('ADMIN')")
    public RouteResponse createRoute(@Valid @RequestBody RouteRequestDto dto) {
        Route route = new Route();
        route.setName(dto.name());
        route.setColor(dto.color());
        Route saved = routeRepository.save(route);
        saveRouteStops(saved, dto.stopIds());
        return routeById(saved.getId());
    }

    @PutMapping("/routes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RouteResponse updateRoute(@PathVariable Long id, @RequestBody RouteRequestDto dto) {
        Route route = routeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Route not found"));
        route.setName(dto.name());
        route.setColor(dto.color());
        Route saved = routeRepository.save(route);
        routeStopRepository.deleteByRouteId(saved.getId());
        saveRouteStops(saved, dto.stopIds());
        return routeById(saved.getId());
    }

    @DeleteMapping("/routes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        scheduleRepository.deleteByRouteId(id);
        routeStopRepository.deleteByRouteId(id);
        routeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/routes/search")
    public List<RouteResponse> searchRoutes(@RequestParam String name) {
        return routeRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(route -> routeById(route.getId()))
                .toList();
    }

    private void saveRouteStops(Route route, List<Long> stopIds) {
        for (int i = 0; i < stopIds.size(); i++) {
            Long stopId = stopIds.get(i);
            Stop stop = stopRepository.findById(stopId)
                    .orElseThrow(() -> new IllegalArgumentException("Stop not found: " + stopId));
            RouteStop routeStop = new RouteStop();
            routeStop.setRoute(route);
            routeStop.setStop(stop);
            routeStop.setStopOrder(i + 1);
            routeStopRepository.save(routeStop);
        }
    }

    // Schedules
    public record ScheduleResponse(
            Long id,
            Long busId,
            Long routeId,
            Long driverId,
            String departureTime,
            Direction direction,
            Set<UserRole> category) {
    }

    @GetMapping("/schedules")
    public List<ScheduleResponse> allSchedules() {
        return scheduleRepository.findAll().stream().map(this::toScheduleResponse).toList();
    }

    @GetMapping("/schedules/{id}")
    public ScheduleResponse scheduleById(@PathVariable Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
        return toScheduleResponse(schedule);
    }

    @PostMapping("/schedules")
    @PreAuthorize("hasRole('ADMIN')")
    public ScheduleResponse createSchedule(@Valid @RequestBody ScheduleRequestDto dto) {
        Schedule schedule = new Schedule();
        applySchedule(schedule, dto);
        return toScheduleResponse(scheduleRepository.save(schedule));
    }

    @PutMapping("/schedules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ScheduleResponse updateSchedule(@PathVariable Long id, @RequestBody ScheduleRequestDto dto) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
        applySchedule(schedule, dto);
        return toScheduleResponse(scheduleRepository.save(schedule));
    }

    @DeleteMapping("/schedules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        scheduleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/schedules/by-bus/{busId}")
    public List<ScheduleResponse> byBus(@PathVariable Long busId) {
        Bus bus = busRepository.findById(busId).orElseThrow(() -> new IllegalArgumentException("Bus not found"));
        return scheduleRepository.findByBus(bus).stream().map(this::toScheduleResponse).toList();
    }

    @GetMapping("/schedules/by-route/{routeId}")
    public List<ScheduleResponse> byRoute(@PathVariable Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route not found"));
        return scheduleRepository.findByRoute(route).stream().map(this::toScheduleResponse).toList();
    }

    @GetMapping("/schedules/by-driver/{driverId}")
    public List<ScheduleResponse> byDriver(@PathVariable Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
        return scheduleRepository.findByDriver(driver).stream().map(this::toScheduleResponse).toList();
    }

    private void applySchedule(Schedule schedule, ScheduleRequestDto dto) {
        Bus bus = busRepository.findById(dto.busId()).orElseThrow(() -> new IllegalArgumentException("Bus not found"));
        Route route = routeRepository.findById(dto.routeId())
                .orElseThrow(() -> new IllegalArgumentException("Route not found"));
        Driver driver = driverRepository.findById(dto.driverId())
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        schedule.setBus(bus);
        schedule.setRoute(route);
        schedule.setDriver(driver);
        schedule.setDepartureTime(LocalTime.parse(dto.departureTime()));
        schedule.setDirection(dto.direction());
        schedule.setCategories(new HashSet<>(dto.category()));
    }

    private ScheduleResponse toScheduleResponse(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getBus().getId(),
                schedule.getRoute().getId(),
                schedule.getDriver().getId(),
                schedule.getDepartureTime().toString(),
                schedule.getDirection(),
                schedule.getCategories());
    }

    // Users
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> allUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public User userById(@PathVariable Long id) {
        return userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @GetMapping("/users/by-username/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public User byUsername(@PathVariable String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public User updateUser(@PathVariable Long id, @RequestBody UserUpdateDto dto) {
        User user = userById(id);
        if (dto.fullName() != null) {
            user.setFullName(dto.fullName());
        }
        if (dto.email() != null) {
            user.setEmail(dto.email());
        }
        if (dto.role() != null) {
            user.setRole(dto.role());
        }
        return userRepository.save(user);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        ambulanceRequestRepository.deleteByRequesterId(id);
        busRequestRepository.deleteByRequesterId(id);
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
