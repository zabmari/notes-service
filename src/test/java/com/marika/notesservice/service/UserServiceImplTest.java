package com.marika.notesservice.service;

import com.marika.notesservice.dto.user.LoginRequest;
import com.marika.notesservice.dto.user.LoginResponse;
import com.marika.notesservice.dto.user.RegisterRequest;
import com.marika.notesservice.dto.user.RegisterResponse;
import com.marika.notesservice.exception.InvalidCredentialsException;
import com.marika.notesservice.exception.InvalidRegisterDataException;
import com.marika.notesservice.exception.LoginAlreadyTakenException;
import com.marika.notesservice.mapper.UserMapper;
import com.marika.notesservice.model.User;
import com.marika.notesservice.repository.UserRepository;
import com.marika.notesservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    UserRepository userRepository;

    @Mock
    UserMapper userMapper;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    @InjectMocks
    UserServiceImpl userService;

    @Test
    void shouldThrowWhenLoginTooShort() {
        RegisterRequest request = new RegisterRequest("ab", "password123");

        assertThrows(InvalidRegisterDataException.class, () -> userService.register(request));
    }

    @Test
    void shouldThrowWhenLoginTooLong() {
        String login = "a".repeat(65);
        RegisterRequest request = new RegisterRequest(login, "password123");

        assertThrows(InvalidRegisterDataException.class, () -> userService.register(request));
    }

    @Test
    void shouldThrowWhenPasswordTooShort() {
        RegisterRequest request = new RegisterRequest("test_user", "123");

        assertThrows(InvalidRegisterDataException.class, () -> userService.register(request));
    }

    @Test
    void shouldThrowWhenLoginAlreadyTaken() {
        RegisterRequest request = new RegisterRequest("test_user", "password123");

        when(userRepository.findByLogin("test_user")).thenReturn(Optional.of(new User()));

        assertThrows(LoginAlreadyTakenException.class, () -> userService.register(request));
    }

    @Test
    void shouldEncodePasswordAndSaveUser() {
        RegisterRequest request = new RegisterRequest("test_user", "password123");

        User user = new User();

        when(userRepository.findByLogin("test_user")).thenReturn(Optional.empty());
        when(userMapper.toEntity(request)).thenReturn(user);
        when(passwordEncoder.encode("password123")).thenReturn("ENCODED");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toRegisterResponse(any()))
                .thenReturn(new RegisterResponse(UUID.randomUUID(), "test_user", Instant.now()));

        RegisterResponse response = userService.register(request);

        assertEquals("test_user", response.login());

        verify(userRepository).save(argThat(saved ->
                saved.getPassword().equals("ENCODED")
                        && saved.getCreatedAt() != null
        ));
    }

    @Test
    void shouldThrowWhenLoginOrPasswordNull() {
        LoginRequest request = new LoginRequest(null, null);

        assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        LoginRequest request = new LoginRequest("test_user", "password123");

        when(userRepository.findByLogin("test_user")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
    }

    @Test
    void shouldThrowWhenPasswordDoesNotMatch() {
        LoginRequest request = new LoginRequest("test_user", "wrong");

        User user = new User();
        user.setLogin("test_user");
        user.setPassword("ENCODED");

        when(userRepository.findByLogin("test_user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "ENCODED")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
    }

    @Test
    void shouldReturnTokenWhenCredentialsValid() {
        LoginRequest request = new LoginRequest("test_user", "password123");

        User user = new User();
        user.setLogin("test_user");
        user.setPassword("ENCODED");

        when(userRepository.findByLogin("test_user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "ENCODED")).thenReturn(true);
        when(jwtService.generateToken("test_user")).thenReturn("TOKEN123");
        when(jwtService.getExpirationTimeSeconds()).thenReturn(3600L);

        LoginResponse response = userService.login(request);

        assertEquals("TOKEN123", response.token());
        assertEquals(3600L, response.expiresIn());
    }
}
