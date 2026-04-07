package com.cuet_transport_backend.dto;

public class CommonDtos {

    public record IdNameDto(Long id, String name) {
    }

    public record LiveLocationDto(
            Double latitude,
            Double longitude,
            java.time.OffsetDateTime timestamp,
            Double heading,
            Double speed) {
    }
}
