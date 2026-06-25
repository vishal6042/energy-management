package com.dems.orchestrator.assistant.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Client confirmation that one or more drafted actions should now be executed, in order. */
public record ConfirmRequest(@NotEmpty List<PendingAction> actions) {
}
