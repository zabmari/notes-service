package com.marika.notesservice.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank
        @Size(min = 3, max = 64)
        String login,

        @NotBlank
        @Size(min = 8, max = 255)
        String password
) {}
