package com.dems.backend.dto;

public record SavingsRecordDto(
        String period,
        double costSaved,
        double energySavedKwh,
        double usageKwh,
        double usageCost) {
}
