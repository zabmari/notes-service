package com.marika.notesservice.repository;

import com.marika.notesservice.model.Item;
import com.marika.notesservice.model.ItemPermission;
import com.marika.notesservice.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemPermissionRepository extends JpaRepository<ItemPermission, UUID> {

    @Query("SELECT p FROM ItemPermission p JOIN p.item i "
            + " WHERE p.user = :user AND i.deleted = false")
    List<ItemPermission> findByUser(@Param("user") User user);

    Optional<ItemPermission> findByItemAndUser(Item item, User user);

    Optional<ItemPermission> findByItemIdAndUserId(UUID itemId, UUID userId);

    void deleteByItem(Item item);
}
