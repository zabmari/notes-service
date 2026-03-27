package com.marika.notesservice.dto.item;

public record UpdateItemRequest(String title,
                                String content,
                                Integer version) {
}
