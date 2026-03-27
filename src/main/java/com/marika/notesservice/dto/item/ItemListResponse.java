package com.marika.notesservice.dto.item;

import java.time.Instant;
import java.util.UUID;

public record ItemWithRole(UUID id,
                           String title,
                           String content,
                           Integer version,
                           UUID ownerId,
                           String myRole,
                           Instant updatedAt) {
}
