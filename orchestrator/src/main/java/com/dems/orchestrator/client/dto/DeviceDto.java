package com.dems.orchestrator.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Mirror of the backend's Device JSON. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeviceDto(
        String id,
        String name,
        String location,
        String model,
        String status,
        double currentTempC,
        double setpointC,
        int powerW,
        int nominalPowerW,
        String algorithm) {
}
