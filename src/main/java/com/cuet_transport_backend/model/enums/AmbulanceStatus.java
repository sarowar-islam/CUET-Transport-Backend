package com.cuet_transport_backend.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AmbulanceStatus {
    AVAILABLE("available"),
    ON_DUTY("on_duty"),
    MAINTENANCE("maintenance");

    private final String value;

    AmbulanceStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AmbulanceStatus fromValue(String value) {
        for (AmbulanceStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ambulance status: " + value);
    }
}
