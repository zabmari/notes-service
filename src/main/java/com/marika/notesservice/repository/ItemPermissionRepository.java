package com.marika.notesservice.repository;

import com.marika.notesservice.model.Item;
import com.marika.notesservice.model.ItemPermission;
import com.marika.notesservice.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemPermissionRepository extends JpaRepository<ItemPermission, UUID> {

    List<ItemPermission> findByItem(Item item);

    List<ItemPermission> findByUser(User user);

    Optional<ItemPermission> findByItemAndUser(Item item, User user);

    boolean existsByItemAndUser(Item item, User user);

}
