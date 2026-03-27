package com.marika.notesservice.dto.item;

import java.time.Instant;
import java.util.UUID;

public record ItemResponse(UUID id,
                           String title,
                           String content,
                           Integer version,
                           UUID ownerId,
                           Instant createdAt,
                           Instant updatedAt) {
}
