package com.cuet_transport_backend.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TransportType {
    BUS("bus"),
    MINIBUS("minibus"),
    CAR("car"),
    AMBULANCE("ambulance");

    private final String value;

    TransportType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TransportType fromValue(String value) {
        for (TransportType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown transport type: " + value);
    }
}
