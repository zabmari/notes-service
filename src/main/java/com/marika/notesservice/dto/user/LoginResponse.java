package com.marika.notesservice.dto.user;

public record LoginResponse(String token,
                            long expiresIn) {
}
