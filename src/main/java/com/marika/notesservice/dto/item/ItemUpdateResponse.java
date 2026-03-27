package com.marika.notesservice.dto.item;

import java.time.Instant;
import java.util.UUID;

public record ItemUpdateResponse(UUID id,
                                 String title,
                                 String content,
                                 Integer version,
                                 Instant updatedAt) {
}
