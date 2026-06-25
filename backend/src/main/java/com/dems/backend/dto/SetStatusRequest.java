package com.dems.backend.dto;

import com.dems.backend.domain.DeviceStatus;
import jakarta.validation.constraints.NotNull;

public record SetStatusRequest(@NotNull DeviceStatus status) {
}
