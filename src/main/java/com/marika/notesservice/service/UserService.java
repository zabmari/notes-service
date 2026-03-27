package com.marika.notesservice.service;

import com.marika.notesservice.dto.user.LoginRequest;
import com.marika.notesservice.dto.user.LoginResponse;
import com.marika.notesservice.dto.user.RegisterRequest;
import com.marika.notesservice.dto.user.RegisterResponse;

public interface UserService {

    RegisterResponse register(RegisterRequest registerRequest);

    LoginResponse login(LoginRequest loginRequest);
}
