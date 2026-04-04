package com.marika.notesservice.dto.item;

import java.time.Instant;
import java.util.UUID;

public record ShareResponse(UUID itemId,
                            UUID userId,
                            String role,
                            Instant grantedAt,
                            @com.fasterxml.jackson.annotation.JsonIgnore boolean isNew) {
}
