package com.dems.backend.dto;

import com.dems.backend.domain.AlgorithmType;
import jakarta.validation.constraints.NotNull;

public record ApplyAlgorithmRequest(@NotNull AlgorithmType algorithm) {
}
