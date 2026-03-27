package com.marika.notesservice.repository;

import com.marika.notesservice.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {
}
