package com.cuet_transport_backend.config;

import com.cuet_transport_backend.model.Ambulance;
import com.cuet_transport_backend.model.AmbulanceLocation;
import com.cuet_transport_backend.model.AmbulanceRequest;
import com.cuet_transport_backend.model.Bus;
import com.cuet_transport_backend.model.BusRequest;
import com.cuet_transport_backend.model.Driver;
import com.cuet_transport_backend.model.Route;
import com.cuet_transport_backend.model.RouteStop;
import com.cuet_transport_backend.model.Schedule;
import com.cuet_transport_backend.model.Stop;
import com.cuet_transport_backend.model.User;
import com.cuet_transport_backend.model.enums.AmbulanceRequestStatus;
import com.cuet_transport_backend.model.enums.AmbulanceStatus;
import com.cuet_transport_backend.model.enums.BusRequestStatus;
import com.cuet_transport_backend.model.enums.Direction;
import com.cuet_transport_backend.model.enums.EmergencyType;
import com.cuet_transport_backend.model.enums.TransportType;
import com.cuet_transport_backend.model.enums.UserRole;
import com.cuet_transport_backend.repository.AmbulanceLocationRepository;
import com.cuet_transport_backend.repository.AmbulanceRepository;
import com.cuet_transport_backend.repository.AmbulanceRequestRepository;
import com.cuet_transport_backend.repository.BusRepository;
import com.cuet_transport_backend.repository.BusRequestRepository;
import com.cuet_transport_backend.repository.DriverRepository;
import com.cuet_transport_backend.repository.RouteRepository;
import com.cuet_transport_backend.repository.RouteStopRepository;
import com.cuet_transport_backend.repository.ScheduleRepository;
import com.cuet_transport_backend.repository.StopRepository;
import com.cuet_transport_backend.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StopRepository stopRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final DriverRepository driverRepository;
    private final BusRepository busRepository;
    private final ScheduleRepository scheduleRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final AmbulanceLocationRepository ambulanceLocationRepository;
    private final AmbulanceRequestRepository ambulanceRequestRepository;
    private final BusRequestRepository busRequestRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        Map<String, User> users = seedUsers();
        Map<String, Stop> stops = seedStops();
        Map<String, Route> routes = seedRoutes(stops);
        Map<String, Driver> drivers = seedDrivers();
        Map<String, Bus> buses = seedBuses();
        Map<String, Ambulance> ambulances = seedAmbulances();

        seedSchedules(buses, routes, drivers);
        seedAmbulanceRequests(users, ambulances);
        seedBusRequests(users, buses, drivers);
    }

    private Map<String, User> seedUsers() {
        Map<String, User> map = new HashMap<>();
        map.put("1", createUser("student1", "student1@cuet.ac.bd", "Rahim Ahmed", UserRole.STUDENT, "student123"));
        map.put("2", createUser("teacher1", "teacher1@cuet.ac.bd", "Dr. Karim Rahman", UserRole.TEACHER, "teacher123"));
        map.put("3", createUser("staff1", "staff1@cuet.ac.bd", "Abdul Hasan", UserRole.STAFF, "staff123"));
        map.put("4", createUser("admin", "admin@cuet.ac.bd", "System Admin", UserRole.ADMIN, "admin123"));
        return map;
    }

    private User createUser(String username, String email, String fullName, UserRole role, String rawPassword) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setIsVerified(Boolean.TRUE);
        return userRepository.save(user);
    }

    private Map<String, Stop> seedStops() {
        Map<String, Stop> map = new HashMap<>();
        map.put("s1", createStop("CUET", 22.4617, 91.9714));
        map.put("s2", createStop("Rastar Matha", 22.3702, 91.8334));
        map.put("s3", createStop("Bahaddarhat", 22.3530, 91.8200));
        map.put("s4", createStop("Muradpur", 22.3600, 91.8150));
        map.put("s5", createStop("2 No Gate", 22.3550, 91.8100));
        map.put("s6", createStop("GEC", 22.3590, 91.8180));
        map.put("s7", createStop("Lalkhan Bazar", 22.3480, 91.8320));
        map.put("s8", createStop("New Market", 22.3420, 91.8350));
        map.put("s9", createStop("KUESH", 22.3700, 91.8000));
        map.put("s10", createStop("Oxygen", 22.3650, 91.8100));
        return map;
    }

    private Stop createStop(String name, double lat, double lng) {
        Stop stop = new Stop();
        stop.setName(name);
        stop.setLatitude(lat);
        stop.setLongitude(lng);
        return stopRepository.save(stop);
    }

    private Map<String, Route> seedRoutes(Map<String, Stop> stops) {
        Map<String, Route> map = new HashMap<>();
        map.put("r1", createRoute("Main Route (Full)", "#3B82F6", List.of(stops.get("s1"), stops.get("s2"),
                stops.get("s3"), stops.get("s4"), stops.get("s5"), stops.get("s6"), stops.get("s7"), stops.get("s8"))));
        map.put("r2", createRoute("Express Route", "#22C55E", List.of(stops.get("s1"), stops.get("s2"), stops.get("s3"),
                stops.get("s6"), stops.get("s7"), stops.get("s8"))));
        map.put("r3", createRoute("Short Route", "#F59E0B",
                List.of(stops.get("s1"), stops.get("s2"), stops.get("s3"), stops.get("s7"), stops.get("s8"))));
        map.put("r4", createRoute("Oxygen Route", "#8B5CF6", List.of(stops.get("s1"), stops.get("s9"), stops.get("s10"),
                stops.get("s6"), stops.get("s7"), stops.get("s8"))));
        return map;
    }

    private Route createRoute(String name, String color, List<Stop> orderedStops) {
        Route route = new Route();
        route.setName(name);
        route.setColor(color);
        Route saved = routeRepository.save(route);
        for (int i = 0; i < orderedStops.size(); i++) {
            RouteStop rs = new RouteStop();
            rs.setRoute(saved);
            rs.setStop(orderedStops.get(i));
            rs.setStopOrder(i + 1);
            routeStopRepository.save(rs);
        }
        return saved;
    }

    private Map<String, Driver> seedDrivers() {
        Map<String, Driver> map = new HashMap<>();
        map.put("d1", createDriver("Mohammad Ali", "01711-123456"));
        map.put("d2", createDriver("Jamal Uddin", "01811-234567"));
        map.put("d3", createDriver("Rafiq Islam", "01911-345678"));
        map.put("d4", createDriver("Kamal Hossain", "01611-456789"));
        map.put("d5", createDriver("Shafiq Ahmed", "01511-567890"));
        map.put("d6", createDriver("Noor Mohammad", "01411-678901"));
        map.put("d7", createDriver("Habib Rahman", "01311-789012"));
        map.put("d8", createDriver("Fazlul Haque", "01711-890123"));
        return map;
    }

    private Driver createDriver(String name, String phone) {
        Driver driver = new Driver();
        driver.setName(name);
        driver.setPhone(phone);
        return driverRepository.save(driver);
    }

    private Map<String, Bus> seedBuses() {
        Map<String, Bus> map = new HashMap<>();
        map.put("b1", createBus("Padma", 52, "চট্ট-ম-১১-১২৩৪"));
        map.put("b2", createBus("Meghna", 52, "চট্ট-ম-১১-২৩৪৫"));
        map.put("b3", createBus("Jamuna", 52, "চট্ট-ম-১১-৩৪৫৬"));
        map.put("b4", createBus("Surma", 48, "চট্ট-ম-১১-৪৫৬৭"));
        map.put("b5", createBus("Karnaphuli", 48, "চট্ট-ম-১১-৫৬৭৮"));
        map.put("b6", createBus("Brahmaputra", 52, "চট্ট-ম-১১-৬৭৮৯"));
        map.put("b7", createBus("Teesta", 48, "চট্ট-ম-১১-৭৮৯০"));
        map.put("b8", createBus("Matamuhuri", 44, "চট্ট-ম-১১-৮৯০১"));
        map.put("b9", createBus("Sangu", 44, "চট্ট-ম-১১-৯০১২"));
        map.put("b10", createBus("Halda", 44, "চট্ট-ম-১১-০১২৩"));
        map.put("b11", createBus("Kushiyara", 52, "চট্ট-ম-১২-১২৩৪"));
        map.put("b12", createBus("Rupsha", 48, "চট্ট-ম-১২-২৩৪৫"));
        map.put("b13", createBus("Shitalakshya", 52, "চট্ট-ম-১২-৩৪৫৬"));
        map.put("b14", createBus("Buriganga", 48, "চট্ট-ম-১২-৪৫৬৭"));
        map.put("b15", createBus("Dhaleshwari", 44, "চট্ট-ম-১২-৫৬৭৮"));
        map.put("b16", createBus("Gorai", 44, "চট্ট-ম-১২-৬৭৮৯"));
        return map;
    }

    private Bus createBus(String name, int capacity, String plate) {
        Bus bus = new Bus();
        bus.setName(name);
        bus.setCapacity(capacity);
        bus.setPlateNumber(plate);
        return busRepository.save(bus);
    }

    private void seedSchedules(Map<String, Bus> buses, Map<String, Route> routes, Map<String, Driver> drivers) {
        createSchedule(buses.get("b1"), routes.get("r1"), drivers.get("d1"), "05:30", Direction.FROM_CUET,
                Set.of(UserRole.STUDENT, UserRole.TEACHER, UserRole.STAFF));
        createSchedule(buses.get("b2"), routes.get("r2"), drivers.get("d2"), "05:30", Direction.FROM_CUET,
                Set.of(UserRole.STUDENT));
        createSchedule(buses.get("b3"), routes.get("r3"), drivers.get("d3"), "05:30", Direction.FROM_CUET,
                Set.of(UserRole.TEACHER, UserRole.STAFF));

        createSchedule(buses.get("b1"), routes.get("r1"), drivers.get("d1"), "07:00", Direction.TO_CUET,
                Set.of(UserRole.STUDENT, UserRole.TEACHER, UserRole.STAFF));
        createSchedule(buses.get("b4"), routes.get("r2"), drivers.get("d4"), "07:00", Direction.TO_CUET,
                Set.of(UserRole.STUDENT));
        createSchedule(buses.get("b5"), routes.get("r3"), drivers.get("d5"), "07:00", Direction.TO_CUET,
                Set.of(UserRole.STUDENT));
        createSchedule(buses.get("b6"), routes.get("r4"), drivers.get("d6"), "07:00", Direction.TO_CUET,
                Set.of(UserRole.TEACHER, UserRole.STAFF));

        createSchedule(buses.get("b7"), routes.get("r1"), drivers.get("d7"), "13:00", Direction.FROM_CUET,
                Set.of(UserRole.STUDENT));
        createSchedule(buses.get("b8"), routes.get("r2"), drivers.get("d8"), "13:00", Direction.FROM_CUET,
                Set.of(UserRole.TEACHER, UserRole.STAFF));

        createSchedule(buses.get("b9"), routes.get("r1"), drivers.get("d1"), "14:00", Direction.FROM_CUET,
                Set.of(UserRole.STUDENT));
        createSchedule(buses.get("b10"), routes.get("r3"), drivers.get("d2"), "14:00", Direction.FROM_CUET,
                Set.of(UserRole.STUDENT));

        createSchedule(buses.get("b11"), routes.get("r1"), drivers.get("d3"), "17:00", Direction.FROM_CUET,
                Set.of(UserRole.STUDENT, UserRole.TEACHER, UserRole.STAFF));
        createSchedule(buses.get("b12"), routes.get("r2"), drivers.get("d4"), "17:00", Direction.FROM_CUET,
                Set.of(UserRole.STUDENT));
        createSchedule(buses.get("b13"), routes.get("r4"), drivers.get("d5"), "17:00", Direction.FROM_CUET,
                Set.of(UserRole.TEACHER, UserRole.STAFF));

        createSchedule(buses.get("b14"), routes.get("r1"), drivers.get("d6"), "21:00", Direction.TO_CUET,
                Set.of(UserRole.STUDENT, UserRole.TEACHER, UserRole.STAFF));
        createSchedule(buses.get("b15"), routes.get("r2"), drivers.get("d7"), "21:00", Direction.TO_CUET,
                Set.of(UserRole.STUDENT));
    }

    private void createSchedule(Bus bus, Route route, Driver driver, String time, Direction direction,
            Set<UserRole> categories) {
        Schedule schedule = new Schedule();
        schedule.setBus(bus);
        schedule.setRoute(route);
        schedule.setDriver(driver);
        schedule.setDepartureTime(LocalTime.parse(time));
        schedule.setDirection(direction);
        schedule.setCategories(categories);
        scheduleRepository.save(schedule);
    }

    private Map<String, Ambulance> seedAmbulances() {
        Map<String, Ambulance> map = new HashMap<>();

        Ambulance a1 = createAmbulance("চট্ট-এম-০১-০০০১", "Rashid Khan", "01700-111111", AmbulanceStatus.AVAILABLE);
        Ambulance a2 = createAmbulance("চট্ট-এম-০১-০০০২", "Salam Mia", "01700-222222", AmbulanceStatus.AVAILABLE);
        Ambulance a3 = createAmbulance("চট্ট-এম-০১-০০০৩", "Iqbal Hossain", "01700-333333", AmbulanceStatus.ON_DUTY);

        map.put("amb1", a1);
        map.put("amb2", a2);
        map.put("amb3", a3);

        saveAmbulanceLocation(a1, 22.4617, 91.9714, OffsetDateTime.now(ZoneOffset.UTC));
        saveAmbulanceLocation(a2, 22.3702, 91.8334, OffsetDateTime.now(ZoneOffset.UTC));
        saveAmbulanceLocation(a3, 22.3530, 91.8200, OffsetDateTime.now(ZoneOffset.UTC));

        return map;
    }

    private Ambulance createAmbulance(String vehicleNo, String driverName, String driverPhone, AmbulanceStatus status) {
        Ambulance ambulance = new Ambulance();
        ambulance.setVehicleNumber(vehicleNo);
        ambulance.setDriverName(driverName);
        ambulance.setDriverPhone(driverPhone);
        ambulance.setStatus(status);
        return ambulanceRepository.save(ambulance);
    }

    private void saveAmbulanceLocation(Ambulance ambulance, double lat, double lng, OffsetDateTime timestamp) {
        AmbulanceLocation loc = new AmbulanceLocation();
        loc.setAmbulance(ambulance);
        loc.setLatitude(lat);
        loc.setLongitude(lng);
        loc.setTimestamp(timestamp);
        ambulanceLocationRepository.save(loc);
    }

    private void seedAmbulanceRequests(Map<String, User> users, Map<String, Ambulance> ambulances) {
        AmbulanceRequest req = new AmbulanceRequest();
        req.setRequester(users.get("1"));
        req.setRequesterName("Rahim Ahmed");
        req.setRequesterPhone("01800-123456");
        req.setRequesterRole(UserRole.STUDENT);
        req.setPickupLocation("Shaheed Abdur Rab Hall");
        req.setEmergencyType(EmergencyType.MEDICAL);
        req.setDescription("High fever and difficulty breathing");
        req.setStatus(AmbulanceRequestStatus.COMPLETED);
        req.setAmbulance(ambulances.get("amb1"));
        req.setCreatedAt(OffsetDateTime.parse("2026-01-19T10:30:00Z"));
        req.setUpdatedAt(OffsetDateTime.parse("2026-01-19T11:00:00Z"));
        ambulanceRequestRepository.save(req);
    }

    private void seedBusRequests(Map<String, User> users, Map<String, Bus> buses, Map<String, Driver> drivers) {
        BusRequest r1 = new BusRequest();
        r1.setRequester(users.get("2"));
        r1.setRequesterName("Dr. Karim Rahman");
        r1.setRequesterRole(UserRole.TEACHER);
        r1.setPurpose("Educational Tour to Cox's Bazar");
        r1.setDate(LocalDate.parse("2026-01-25"));
        r1.setStartTime(LocalTime.parse("06:00"));
        r1.setEndTime(LocalTime.parse("22:00"));
        r1.setPickupLocation("CUET Main Gate");
        r1.setDestination("Cox's Bazar");
        r1.setExpectedPassengers(45);
        r1.setStatus(BusRequestStatus.APPROVED);
        r1.setTransportType(TransportType.BUS);
        r1.setAssignedBus(buses.get("b1"));
        r1.setAssignedDriver(drivers.get("d1"));
        r1.setCreatedAt(OffsetDateTime.parse("2026-01-15T09:00:00Z"));
        r1.setUpdatedAt(OffsetDateTime.parse("2026-01-16T14:00:00Z"));
        busRequestRepository.save(r1);

        BusRequest r2 = new BusRequest();
        r2.setRequester(users.get("3"));
        r2.setRequesterName("Abdul Hasan");
        r2.setRequesterRole(UserRole.STAFF);
        r2.setPurpose("Staff Picnic");
        r2.setDate(LocalDate.parse("2026-01-28"));
        r2.setStartTime(LocalTime.parse("08:00"));
        r2.setEndTime(LocalTime.parse("18:00"));
        r2.setPickupLocation("CUET Admin Building");
        r2.setDestination("Foy's Lake");
        r2.setExpectedPassengers(30);
        r2.setStatus(BusRequestStatus.PENDING);
        r2.setTransportType(TransportType.BUS);
        r2.setCreatedAt(OffsetDateTime.parse("2026-01-18T11:00:00Z"));
        r2.setUpdatedAt(OffsetDateTime.parse("2026-01-18T11:00:00Z"));
        busRequestRepository.save(r2);
    }
}
