package com.marika.notesservice.dto.item;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record ShareRequest(
        @NotNull
        UUID userId,

        @NotBlank
        @Pattern(regexp = "VIEWER|EDITOR")
        String role) {
}
