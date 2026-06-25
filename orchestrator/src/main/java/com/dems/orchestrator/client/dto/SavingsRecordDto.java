package com.dems.orchestrator.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SavingsRecordDto(
        String period,
        double costSaved,
        double energySavedKwh,
        double usageKwh,
        double usageCost) {
}
