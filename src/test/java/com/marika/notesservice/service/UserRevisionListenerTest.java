package com.marika.notesservice.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.marika.notesservice.audit.CustomRevisionEntity;
import com.marika.notesservice.audit.UserRevisionListener;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserRevisionListenerTest {

    @Test
    void shouldNotThrowWhenSecurityContextIsEmpty() {

        SecurityContextHolder.clearContext();
        UserRevisionListener listener = new UserRevisionListener();

        assertDoesNotThrow(() -> listener.newRevision(new CustomRevisionEntity()));
    }
}
