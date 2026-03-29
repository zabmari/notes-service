package com.marika.notesservice.exception;

public class SelfShareException extends RuntimeException {
    public SelfShareException(String message) {
        super(message);
    }
}
