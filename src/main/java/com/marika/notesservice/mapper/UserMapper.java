package com.marika.notesservice.mapper;

import com.marika.notesservice.dto.user.RegisterRequest;
import com.marika.notesservice.dto.user.RegisterResponse;
import com.marika.notesservice.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(RegisterRequest registerRequest);

    RegisterResponse toRegisterResponse(User user);

}
