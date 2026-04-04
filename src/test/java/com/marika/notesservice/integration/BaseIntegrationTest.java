package com.marika.notesservice.integration;

import com.marika.notesservice.model.User;
import com.marika.notesservice.repository.ItemPermissionRepository;
import com.marika.notesservice.repository.ItemRepository;
import com.marika.notesservice.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    static final MySQLContainer<?> mysql;

    static {
        mysql = new MySQLContainer<>("mysql:8.0.32").withDatabaseName("notes").withUsername("root")
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
    @Autowired
    protected EntityManager entityManager;
    @Autowired
    protected TransactionTemplate transactionTemplate;

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

        transactionTemplate.execute(status -> {
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();

            entityManager.createNativeQuery("TRUNCATE TABLE item_permissions").executeUpdate();
            entityManager.createNativeQuery("TRUNCATE TABLE items").executeUpdate();
            entityManager.createNativeQuery("TRUNCATE TABLE users").executeUpdate();

            entityManager.createNativeQuery("TRUNCATE TABLE items_aud").executeUpdate();
            entityManager.createNativeQuery("TRUNCATE TABLE custom_revision_entity")
                    .executeUpdate();

            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
            
            User user = new User();
            user.setLogin("test-user");
            user.setPassword("password");
            userRepository.saveAndFlush(user);

            return null;
        });
    }
}