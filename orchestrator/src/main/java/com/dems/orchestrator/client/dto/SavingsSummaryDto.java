package com.dems.orchestrator.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SavingsSummaryDto(
        double totalCostSaved,
        double totalEnergySavedKwh,
        double totalUsageKwh,
        double totalUsageCost,
        List<SavingsRecordDto> daily,
        List<SavingsRecordDto> monthly) {
}
