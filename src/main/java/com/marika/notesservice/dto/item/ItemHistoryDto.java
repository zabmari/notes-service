package com.marika.notesservice.dto.item;

import java.time.Instant;

public record ItemHistoryDto(int revision,
                             String revisionType,
                             Instant timestamp,
                             String changedBy,
                             String title,
                             String content) {
}
