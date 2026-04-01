package com.marika.notesservice.service;

import com.marika.notesservice.dto.item.CreateItemRequest;
import com.marika.notesservice.dto.item.ItemHistoryResponse;
import com.marika.notesservice.dto.item.ItemListResponse;
import com.marika.notesservice.dto.item.ItemResponse;
import com.marika.notesservice.dto.item.ItemUpdateRequest;
import com.marika.notesservice.dto.item.ItemUpdateResponse;
import com.marika.notesservice.dto.item.ShareRequest;
import com.marika.notesservice.dto.item.ShareResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

public interface ItemService {

    ItemResponse createItem(CreateItemRequest createItemRequest);

    List<ItemListResponse> getItemsForCurrentUser();

    ItemUpdateResponse updateItem(UUID id, ItemUpdateRequest updateItemRequest);

    void softDeleteItem(UUID id);

    List<ItemHistoryResponse> getItemHistory(UUID id);

    ResponseEntity<ShareResponse> shareItem(UUID id, ShareRequest shareRequest);

    void revokeShare(UUID itemId, UUID targetUserId);

}
