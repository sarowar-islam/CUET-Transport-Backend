package com.cuet_transport_backend.model;

import com.cuet_transport_backend.model.enums.AmbulanceRequestStatus;
import com.cuet_transport_backend.model.enums.EmergencyType;
import com.cuet_transport_backend.model.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ambulance_requests", indexes = {
        @Index(name = "idx_ambulance_requests_status", columnList = "status"),
        @Index(name = "idx_ambulance_requests_ambulance_id", columnList = "ambulance_id")
})
@Getter
@Setter
@NoArgsConstructor
public class AmbulanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(nullable = false, length = 200)
    private String requesterName;

    @Column(nullable = false, length = 50)
    private String requesterPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole requesterRole;

    @Column(nullable = false, length = 300)
    private String pickupLocation;

    @Column
    private Double pickupLatitude;

    @Column
    private Double pickupLongitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmergencyType emergencyType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AmbulanceRequestStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ambulance_id")
    private Ambulance ambulance;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
