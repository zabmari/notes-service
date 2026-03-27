package com.marika.notesservice.dto.item;

public record ItemUpdateRequest(String title,
                                String content,
                                Integer version) {
}
