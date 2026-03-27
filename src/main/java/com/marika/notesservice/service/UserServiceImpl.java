package com.marika.notesservice.service;

import com.marika.notesservice.dto.user.LoginRequest;
import com.marika.notesservice.dto.user.LoginResponse;
import com.marika.notesservice.dto.user.RegisterRequest;
import com.marika.notesservice.dto.user.RegisterResponse;
import com.marika.notesservice.mapper.UserMapper;
import com.marika.notesservice.model.User;
import com.marika.notesservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService{
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public RegisterResponse register(RegisterRequest registerRequest) {
        if (registerRequest.login() == null || registerRequest.login().length() < 3 || registerRequest.login().length() > 64) {
            throw new ResponseStatusException(BAD_REQUEST, "Login must be between 3 and 64 characters");
        }

        if (registerRequest.password() == null || registerRequest.password().length() < 8) {
            throw new ResponseStatusException(BAD_REQUEST, "Password must be at least 8 characters long");
        }

        if (userRepository.findByLogin(registerRequest.login()).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "Login already taken");
        }

        User user = userMapper.toEntity(registerRequest);
        user.setPassword(passwordEncoder.encode(registerRequest.password()));
        user.setCreatedAt(Instant.now());
        User savedUser = userRepository.save(user);
        return userMapper.toRegisterResponse(savedUser);
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        if (loginRequest.login() == null || loginRequest.password() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Login and password are required");
        }

        User user = userRepository.findByLogin(loginRequest.login())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED,"Invalid login or password"));

        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid login or password");
        }
        String token = "dummy-token";
        long expiresIn = 3600;

        return new LoginResponse(token, expiresIn);
    }
}
