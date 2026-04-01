package com.marika.notesservice.integration;

import com.marika.notesservice.model.User;
import com.marika.notesservice.repository.ItemPermissionRepository;
import com.marika.notesservice.repository.ItemRepository;
import com.marika.notesservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    static final MySQLContainer<?> mysql;

    static {
        mysql = new MySQLContainer<>("mysql:8.0.32")
                .withDatabaseName("notes")
                .withUsername("root")
                .withPassword("root");
        mysql.start();
    }

    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected ItemRepository itemRepository;
    @Autowired
    protected ItemPermissionRepository itemPermissionRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @BeforeEach
    void cleanUp() {
        SecurityContextHolder.clearContext();

        itemPermissionRepository.deleteAllInBatch();
        itemRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        User user = new User();
        user.setLogin("test-user");
        user.setPassword(passwordEncoder.encode("password"));
        userRepository.saveAndFlush(user);
    }
}