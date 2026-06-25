package com.dems.backend.dto;

import java.util.List;

public record DeviceSavingsDto(
        String deviceId,
        List<SavingsRecordDto> daily,
        List<SavingsRecordDto> monthly) {
}
