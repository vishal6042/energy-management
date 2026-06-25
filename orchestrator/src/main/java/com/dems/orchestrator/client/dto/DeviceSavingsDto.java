package com.dems.orchestrator.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeviceSavingsDto(
        String deviceId,
        List<SavingsRecordDto> daily,
        List<SavingsRecordDto> monthly) {
}
