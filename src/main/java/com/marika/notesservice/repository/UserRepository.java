package com.marika.notesservice.repository;

import com.marika.notesservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository  extends JpaRepository<User, UUID> {

    Optional<User> findByLogin(String login);
}
