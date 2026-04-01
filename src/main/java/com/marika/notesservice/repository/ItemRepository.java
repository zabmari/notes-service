package com.marika.notesservice.repository;

import com.marika.notesservice.model.Item;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, UUID> {
}
