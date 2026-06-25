package com.dems.backend.dto;

import java.util.List;

public record SavingsSummaryDto(
        double totalCostSaved,
        double totalEnergySavedKwh,
        double totalUsageKwh,
        double totalUsageCost,
        List<SavingsRecordDto> daily,
        List<SavingsRecordDto> monthly) {
}
