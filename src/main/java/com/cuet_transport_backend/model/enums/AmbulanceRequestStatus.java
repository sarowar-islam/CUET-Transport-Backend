package com.cuet_transport_backend.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AmbulanceRequestStatus {
    PENDING("pending"),
    ASSIGNED("assigned"),
    EN_ROUTE("en_route"),
    ARRIVED("arrived"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    private final String value;

    AmbulanceRequestStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AmbulanceRequestStatus fromValue(String value) {
        for (AmbulanceRequestStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ambulance request status: " + value);
    }
}
