package com.cuet_transport_backend.controller;

import com.cuet_transport_backend.model.Ambulance;
import com.cuet_transport_backend.model.AmbulanceLocation;
import com.cuet_transport_backend.model.AmbulanceRequest;
import com.cuet_transport_backend.model.Bus;
import com.cuet_transport_backend.model.BusRequest;
import com.cuet_transport_backend.model.Driver;
import com.cuet_transport_backend.model.User;
import com.cuet_transport_backend.model.enums.AmbulanceRequestStatus;
import com.cuet_transport_backend.model.enums.AmbulanceStatus;
import com.cuet_transport_backend.model.enums.BusRequestStatus;
import com.cuet_transport_backend.model.enums.EmergencyType;
import com.cuet_transport_backend.model.enums.TransportType;
import com.cuet_transport_backend.model.enums.UserRole;
import com.cuet_transport_backend.repository.AmbulanceLocationRepository;
import com.cuet_transport_backend.repository.AmbulanceRepository;
import com.cuet_transport_backend.repository.AmbulanceRequestRepository;
import com.cuet_transport_backend.repository.BusRepository;
import com.cuet_transport_backend.repository.BusRequestRepository;
import com.cuet_transport_backend.repository.DriverRepository;
import com.cuet_transport_backend.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
@RequestMapping
@RequiredArgsConstructor
public class RequestControllers {

    private final AmbulanceRepository ambulanceRepository;
    private final AmbulanceLocationRepository ambulanceLocationRepository;
    private final AmbulanceRequestRepository ambulanceRequestRepository;
    private final BusRequestRepository busRequestRepository;
    private final BusRepository busRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;

    public record AmbulanceRequestDto(
            @NotNull Long requesterId,
            @NotBlank String requesterName,
            @NotBlank String requesterPhone,
            @NotNull UserRole requesterRole,
            @NotBlank String pickupLocation,
            Double pickupLatitude,
            Double pickupLongitude,
            @NotNull EmergencyType emergencyType,
            String description,
            AmbulanceRequestStatus status,
            Long ambulanceId) {
    }

    public record BusRequestDto(
            @NotNull Long requesterId,
            @NotBlank String requesterName,
            @NotNull UserRole requesterRole,
            String requesterPhone,
            String requesterPosition,
            String requesterDepartment,
            @NotBlank String purpose,
            String reason,
            TransportType transportType,
            String duration,
            @NotBlank String date,
            @NotBlank String startTime,
            @NotBlank String endTime,
            @NotBlank String pickupLocation,
            @NotBlank String destination,
            @NotNull Integer expectedPassengers,
            BusRequestStatus status,
            Long assignedBusId,
            Long assignedDriverId,
            String adminNotes) {
    }

    public record AmbulanceLocationDto(@NotNull Double latitude, @NotNull Double longitude, Double heading,
            Double speed) {
    }

    // Ambulances
    @GetMapping("/ambulances")
    public List<AmbulanceResponse> allAmbulances() {
        List<Ambulance> ambulances = ambulanceRepository.findAll();
        if (ambulances.isEmpty()) {
            return List.of();
        }

        List<Long> ambulanceIds = ambulances.stream().map(Ambulance::getId).toList();
        Map<Long, AmbulanceLocation> latestLocationByAmbulanceId = new HashMap<>();
        for (AmbulanceLocation location : ambulanceLocationRepository.findLatestByAmbulanceIds(ambulanceIds)) {
            latestLocationByAmbulanceId.putIfAbsent(location.getAmbulance().getId(), location);
        }

        return ambulances.stream()
                .map(ambulance -> toAmbulanceResponse(ambulance, latestLocationByAmbulanceId.get(ambulance.getId())))
                .toList();
    }

    @GetMapping("/ambulances/{id}")
    public AmbulanceResponse byAmbulanceId(@PathVariable Long id) {
        Ambulance ambulance = ambulanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ambulance not found"));
        return toAmbulanceResponse(ambulance);
    }

    @PostMapping("/ambulances")
    @PreAuthorize("hasRole('ADMIN')")
    public AmbulanceResponse createAmbulance(@Valid @RequestBody Ambulance ambulance) {
        Ambulance normalized = normalizeAmbulanceInput(ambulance);

        return ambulanceRepository.findByVehicleNumberIgnoreCase(normalized.getVehicleNumber())
                .map(this::toAmbulanceResponse)
                .orElseGet(() -> {
                    try {
                        return toAmbulanceResponse(ambulanceRepository.save(normalized));
                    } catch (DataIntegrityViolationException ex) {
                        return ambulanceRepository.findByVehicleNumberIgnoreCase(normalized.getVehicleNumber())
                                .map(this::toAmbulanceResponse)
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Ambulance with this vehicle number already exists"));
                    }
                });
    }

    @PutMapping("/ambulances/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AmbulanceResponse updateAmbulance(@PathVariable Long id, @RequestBody Ambulance incoming) {
        Ambulance normalized = normalizeAmbulanceInput(incoming);
        ambulanceRepository.findByVehicleNumberIgnoreCase(normalized.getVehicleNumber())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ambulance with this vehicle number already exists");
                });

        Ambulance ambulance = ambulanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ambulance not found"));
        ambulance.setVehicleNumber(normalized.getVehicleNumber());
        ambulance.setDriverName(normalized.getDriverName());
        ambulance.setDriverPhone(normalized.getDriverPhone());
        ambulance.setStatus(normalized.getStatus());
        return toAmbulanceResponse(ambulanceRepository.save(ambulance));
    }

    @PutMapping("/ambulances/{id}/location")
    @PreAuthorize("hasRole('ADMIN')")
    public AmbulanceResponse updateAmbulanceLocation(
            @PathVariable Long id,
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        Ambulance ambulance = ambulanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ambulance not found"));
        AmbulanceLocation loc = new AmbulanceLocation();
        loc.setAmbulance(ambulance);
        loc.setLatitude(latitude);
        loc.setLongitude(longitude);
        loc.setTimestamp(OffsetDateTime.now());
        ambulanceLocationRepository.save(loc);
        return toAmbulanceResponse(ambulance);
    }

    @DeleteMapping("/ambulances/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> deleteAmbulance(@PathVariable Long id) {
        Ambulance ambulance = ambulanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ambulance not found"));

        ambulanceRequestRepository.clearAmbulanceAssignments(id);
        ambulanceLocationRepository.deleteByAmbulanceId(id);
        ambulanceRepository.delete(ambulance);

        return ResponseEntity.noContent().build();
    }

    // Ambulance requests
    @GetMapping("/ambulance-requests")
    public List<AmbulanceRequestResponse> allAmbulanceRequests() {
        return ambulanceRequestRepository.findAllWithRequesterAndAmbulance().stream()
                .map(this::toAmbulanceRequestResponse)
                .toList();
    }

    @GetMapping("/ambulance-requests/{id}")
    public AmbulanceRequestResponse ambulanceRequestById(@PathVariable Long id) {
        return toAmbulanceRequestResponse(findAmbulanceRequest(id));
    }

    @PostMapping("/ambulance-requests")
    public AmbulanceRequestResponse createAmbulanceRequest(@Valid @RequestBody AmbulanceRequestDto dto) {
        User requester = userRepository.findById(dto.requesterId())
                .orElseThrow(() -> new IllegalArgumentException("Requester not found"));

        AmbulanceRequest req = new AmbulanceRequest();
        req.setRequester(requester);
        req.setRequesterName(dto.requesterName());
        req.setRequesterPhone(dto.requesterPhone());
        req.setRequesterRole(dto.requesterRole());
        req.setPickupLocation(dto.pickupLocation());
        req.setPickupLatitude(dto.pickupLatitude());
        req.setPickupLongitude(dto.pickupLongitude());
        req.setEmergencyType(dto.emergencyType());
        req.setDescription(dto.description());
        req.setStatus(dto.status() == null ? AmbulanceRequestStatus.PENDING : dto.status());

        if (dto.ambulanceId() != null) {
            Ambulance ambulance = ambulanceRepository.findById(dto.ambulanceId())
                    .orElseThrow(() -> new IllegalArgumentException("Ambulance not found"));
            req.setAmbulance(ambulance);
        }

        return toAmbulanceRequestResponse(ambulanceRequestRepository.save(req));
    }

    @GetMapping("/ambulance-requests/by-requester/{requesterId}")
    public List<AmbulanceRequestResponse> ambulanceByRequester(@PathVariable Long requesterId) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Requester not found"));
        return ambulanceRequestRepository.findByRequesterWithRequesterAndAmbulance(requester).stream()
                .map(this::toAmbulanceRequestResponse)
                .toList();
    }

    @GetMapping("/ambulance-requests/by-status/{status}")
    public List<AmbulanceRequestResponse> ambulanceByStatus(@PathVariable String status) {
        AmbulanceRequestStatus parsed = AmbulanceRequestStatus.fromValue(status);
        return ambulanceRequestRepository.findByStatusWithRequesterAndAmbulance(parsed).stream()
                .map(this::toAmbulanceRequestResponse)
                .toList();
    }

    @PutMapping("/ambulance-requests/{requestId}/assign/{ambulanceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public AmbulanceRequestResponse assignAmbulance(@PathVariable Long requestId, @PathVariable Long ambulanceId) {
        AmbulanceRequest req = findAmbulanceRequest(requestId);
        Ambulance ambulance = ambulanceRepository.findById(ambulanceId)
                .orElseThrow(() -> new IllegalArgumentException("Ambulance not found"));

        req.setAmbulance(ambulance);
        req.setStatus(AmbulanceRequestStatus.ASSIGNED);
        ambulance.setStatus(AmbulanceStatus.ON_DUTY);
        ambulanceRepository.save(ambulance);

        return toAmbulanceRequestResponse(ambulanceRequestRepository.save(req));
    }

    @PutMapping("/ambulance-requests/{requestId}/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public AmbulanceRequestResponse updateAmbulanceRequestStatus(
            @PathVariable Long requestId,
            @PathVariable String status) {
        AmbulanceRequestStatus parsed = AmbulanceRequestStatus.fromValue(status);
        AmbulanceRequest req = findAmbulanceRequest(requestId);
        req.setStatus(parsed);
        if ((parsed == AmbulanceRequestStatus.COMPLETED || parsed == AmbulanceRequestStatus.CANCELLED)
                && req.getAmbulance() != null) {
            Ambulance amb = req.getAmbulance();
            amb.setStatus(AmbulanceStatus.AVAILABLE);
            ambulanceRepository.save(amb);
        }
        return toAmbulanceRequestResponse(ambulanceRequestRepository.save(req));
    }

    // Bus requests
    @GetMapping("/bus-requests")
    public List<BusRequestResponse> allBusRequests() {
        return busRequestRepository.findAllWithRequesterAndAssignments().stream()
                .map(this::toBusRequestResponse)
                .toList();
    }

    @GetMapping("/bus-requests/{id}")
    public BusRequestResponse busRequestById(@PathVariable Long id) {
        return toBusRequestResponse(findBusRequest(id));
    }

    @PostMapping("/bus-requests")
    public BusRequestResponse createBusRequest(@Valid @RequestBody BusRequestDto dto) {
        User requester = userRepository.findById(dto.requesterId())
                .orElseThrow(() -> new IllegalArgumentException("Requester not found"));

        BusRequest req = new BusRequest();
        applyBusRequest(req, dto, requester);
        req.setStatus(dto.status() == null ? BusRequestStatus.PENDING : dto.status());
        return toBusRequestResponse(busRequestRepository.save(req));
    }

    @GetMapping("/bus-requests/by-requester/{requesterId}")
    public List<BusRequestResponse> busByRequester(@PathVariable Long requesterId) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Requester not found"));
        return busRequestRepository.findByRequesterWithRequesterAndAssignments(requester).stream()
                .map(this::toBusRequestResponse)
                .toList();
    }

    @GetMapping("/bus-requests/by-status/{status}")
    public List<BusRequestResponse> busByStatus(@PathVariable String status) {
        BusRequestStatus parsed = BusRequestStatus.fromValue(status);
        return busRequestRepository.findByStatusWithRequesterAndAssignments(parsed).stream()
                .map(this::toBusRequestResponse)
                .toList();
    }

    @PutMapping("/bus-requests/{requestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public BusRequestResponse approveBusRequest(
            @PathVariable Long requestId,
            @RequestParam Long busId,
            @RequestParam Long driverId) {
        BusRequest req = findBusRequest(requestId);
        Bus bus = busRepository.findById(busId).orElseThrow(() -> new IllegalArgumentException("Bus not found"));
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
        req.setAssignedBus(bus);
        req.setAssignedDriver(driver);
        req.setStatus(BusRequestStatus.APPROVED);
        return toBusRequestResponse(busRequestRepository.save(req));
    }

    @PutMapping("/bus-requests/{requestId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public BusRequestResponse rejectBusRequest(@PathVariable Long requestId,
            @RequestParam(required = false) String adminNotes) {
        BusRequest req = findBusRequest(requestId);
        req.setStatus(BusRequestStatus.REJECTED);
        req.setAdminNotes(adminNotes);
        return toBusRequestResponse(busRequestRepository.save(req));
    }

    @PutMapping("/bus-requests/{requestId}/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public BusRequestResponse updateBusRequestStatus(@PathVariable Long requestId, @PathVariable String status) {
        BusRequestStatus parsed = BusRequestStatus.fromValue(status);
        BusRequest req = findBusRequest(requestId);
        req.setStatus(parsed);
        return toBusRequestResponse(busRequestRepository.save(req));
    }

    private AmbulanceRequest findAmbulanceRequest(Long id) {
        return ambulanceRequestRepository.findByIdWithRequesterAndAmbulance(id)
                .orElseThrow(() -> new IllegalArgumentException("Ambulance request not found"));
    }

    private BusRequest findBusRequest(Long id) {
        return busRequestRepository.findByIdWithRequesterAndAssignments(id)
                .orElseThrow(() -> new IllegalArgumentException("Bus request not found"));
    }

    private Ambulance normalizeAmbulanceInput(Ambulance incoming) {
        if (incoming == null) {
            throw new IllegalArgumentException("Ambulance payload is required");
        }
        incoming.setVehicleNumber(normalizeText(incoming.getVehicleNumber(), "Vehicle number is required"));
        incoming.setDriverName(normalizeText(incoming.getDriverName(), "Driver name is required"));
        incoming.setDriverPhone(normalizeText(incoming.getDriverPhone(), "Driver phone is required"));
        if (incoming.getStatus() == null) {
            throw new IllegalArgumentException("Ambulance status is required");
        }
        return incoming;
    }

    private String normalizeText(String value, String errorMessage) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return normalized;
    }

    private void applyBusRequest(BusRequest req, BusRequestDto dto, User requester) {
        req.setRequester(requester);
        req.setRequesterName(dto.requesterName());
        req.setRequesterRole(dto.requesterRole());
        req.setRequesterPhone(dto.requesterPhone());
        req.setRequesterPosition(dto.requesterPosition());
        req.setRequesterDepartment(dto.requesterDepartment());
        req.setPurpose(dto.purpose());
        req.setReason(dto.reason());
        req.setTransportType(dto.transportType() == null ? TransportType.BUS : dto.transportType());
        req.setDuration(dto.duration());
        req.setDate(LocalDate.parse(dto.date()));
        req.setStartTime(LocalTime.parse(dto.startTime()));
        req.setEndTime(LocalTime.parse(dto.endTime()));
        req.setPickupLocation(dto.pickupLocation());
        req.setDestination(dto.destination());
        req.setExpectedPassengers(dto.expectedPassengers());
        req.setAdminNotes(dto.adminNotes());

        if (dto.assignedBusId() != null) {
            req.setAssignedBus(busRepository.findById(dto.assignedBusId())
                    .orElseThrow(() -> new IllegalArgumentException("Assigned bus not found")));
        }
        if (dto.assignedDriverId() != null) {
            req.setAssignedDriver(driverRepository.findById(dto.assignedDriverId())
                    .orElseThrow(() -> new IllegalArgumentException("Assigned driver not found")));
        }
    }

    public record AmbulanceResponse(
            Long id,
            String vehicleNumber,
            String driverName,
            String driverPhone,
            AmbulanceStatus status,
            LiveLocationResponse currentLocation) {
    }

    public record LiveLocationResponse(
            Double latitude,
            Double longitude,
            OffsetDateTime timestamp,
            Double heading,
            Double speed) {
    }

    public record AmbulanceRequestResponse(
            Long id,
            Long requesterId,
            String requesterName,
            String requesterPhone,
            UserRole requesterRole,
            String pickupLocation,
            Double pickupLatitude,
            Double pickupLongitude,
            EmergencyType emergencyType,
            String description,
            AmbulanceRequestStatus status,
            Long ambulanceId,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    public record BusRequestResponse(
            Long id,
            Long requesterId,
            String requesterName,
            UserRole requesterRole,
            String requesterPhone,
            String requesterPosition,
            String requesterDepartment,
            String purpose,
            String reason,
            TransportType transportType,
            String duration,
            String date,
            String startTime,
            String endTime,
            String pickupLocation,
            String destination,
            Integer expectedPassengers,
            BusRequestStatus status,
            Long assignedBusId,
            Long assignedDriverId,
            String adminNotes,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    private AmbulanceResponse toAmbulanceResponse(Ambulance ambulance) {
        AmbulanceLocation latest = ambulanceLocationRepository.findTopByAmbulanceOrderByTimestampDesc(ambulance)
                .orElse(null);
        return toAmbulanceResponse(ambulance, latest);
    }

    private AmbulanceResponse toAmbulanceResponse(Ambulance ambulance, AmbulanceLocation latestLocation) {
        LiveLocationResponse current = latestLocation == null
                ? null
                : new LiveLocationResponse(
                        latestLocation.getLatitude(),
                        latestLocation.getLongitude(),
                        latestLocation.getTimestamp(),
                        latestLocation.getHeading(),
                        latestLocation.getSpeed());

        return new AmbulanceResponse(
                ambulance.getId(),
                ambulance.getVehicleNumber(),
                ambulance.getDriverName(),
                ambulance.getDriverPhone(),
                ambulance.getStatus(),
                current);
    }

    private AmbulanceRequestResponse toAmbulanceRequestResponse(AmbulanceRequest req) {
        return new AmbulanceRequestResponse(
                req.getId(),
                req.getRequester().getId(),
                req.getRequesterName(),
                req.getRequesterPhone(),
                req.getRequesterRole(),
                req.getPickupLocation(),
                req.getPickupLatitude(),
                req.getPickupLongitude(),
                req.getEmergencyType(),
                req.getDescription(),
                req.getStatus(),
                req.getAmbulance() == null ? null : req.getAmbulance().getId(),
                req.getCreatedAt(),
                req.getUpdatedAt());
    }

    private BusRequestResponse toBusRequestResponse(BusRequest req) {
        return new BusRequestResponse(
                req.getId(),
                req.getRequester().getId(),
                req.getRequesterName(),
                req.getRequesterRole(),
                req.getRequesterPhone(),
                req.getRequesterPosition(),
                req.getRequesterDepartment(),
                req.getPurpose(),
                req.getReason(),
                req.getTransportType(),
                req.getDuration(),
                req.getDate().toString(),
                req.getStartTime().toString(),
                req.getEndTime().toString(),
                req.getPickupLocation(),
                req.getDestination(),
                req.getExpectedPassengers(),
                req.getStatus(),
                req.getAssignedBus() == null ? null : req.getAssignedBus().getId(),
                req.getAssignedDriver() == null ? null : req.getAssignedDriver().getId(),
                req.getAdminNotes(),
                req.getCreatedAt(),
                req.getUpdatedAt());
    }
}
