package com.marika.notesservice.dto.item;

import java.util.UUID;

public record ShareRequest(UUID userId,
                           String role) {
}
