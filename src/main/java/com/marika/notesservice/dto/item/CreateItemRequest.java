package com.marika.notesservice.dto.item;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateItemRequest(
        @NotBlank
        @Size(min = 1, max = 255)
        String title,

        String content) {
}
