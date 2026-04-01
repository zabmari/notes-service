package com.marika.notesservice.dto.item;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ItemUpdateRequest(
        @Size(min = 1, max = 200)
        String title,

        @Size(max = 10000)
        String content,

        @NotNull
        @PositiveOrZero
        Integer version) {
}

