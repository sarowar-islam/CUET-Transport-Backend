package com.cuet_transport_backend.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BusRequestStatus {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    COMPLETED("completed");

    private final String value;

    BusRequestStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static BusRequestStatus fromValue(String value) {
        for (BusRequestStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown bus request status: " + value);
    }
}
