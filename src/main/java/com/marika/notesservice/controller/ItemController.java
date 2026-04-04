package com.marika.notesservice.controller;

import com.marika.notesservice.dto.item.CreateItemRequest;
import com.marika.notesservice.dto.item.ItemHistoryResponse;
import com.marika.notesservice.dto.item.ItemListResponse;
import com.marika.notesservice.dto.item.ItemResponse;
import com.marika.notesservice.dto.item.ItemUpdateRequest;
import com.marika.notesservice.dto.item.ItemUpdateResponse;
import com.marika.notesservice.dto.item.ShareRequest;
import com.marika.notesservice.dto.item.ShareResponse;
import com.marika.notesservice.service.ItemService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/items")
public class ItemController {
    private final ItemService itemService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse create(@Valid @RequestBody CreateItemRequest request) {
        return itemService.createItem(request);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ItemListResponse> getAll() {
        return itemService.getItemsForCurrentUser();
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ItemUpdateResponse update(@PathVariable UUID id,
                                     @Valid @RequestBody ItemUpdateRequest request) {
        return itemService.updateItem(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        itemService.softDeleteItem(id);
    }

    @GetMapping("/{id}/history")
    @ResponseStatus(HttpStatus.OK)
    public List<ItemHistoryResponse> getHistory(@PathVariable UUID id) {
        return itemService.getItemHistory(id);
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<ShareResponse> share(@PathVariable UUID id,
                                               @Valid @RequestBody ShareRequest request) {
        ShareResponse response = itemService.shareItem(id, request);

        if (response.isNew()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/share/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeShare(@PathVariable UUID id, @PathVariable UUID userId) {
        itemService.revokeShare(id, userId);
    }
}
