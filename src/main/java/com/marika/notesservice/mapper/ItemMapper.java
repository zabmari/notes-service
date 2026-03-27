package com.marika.notesservice.mapper;

import com.marika.notesservice.dto.item.CreateItemRequest;
import com.marika.notesservice.dto.item.ItemResponse;
import com.marika.notesservice.dto.item.ItemUpdateRequest;
import com.marika.notesservice.dto.item.ItemListResponse;
import com.marika.notesservice.dto.item.ItemUpdateResponse;
import com.marika.notesservice.model.Item;
import com.marika.notesservice.model.ItemPermission;
import com.marika.notesservice.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    Item toEntity(CreateItemRequest createItemRequest, User user);

    void updateEntity(@MappingTarget Item item, ItemUpdateRequest updateItemRequest);

    ItemResponse toResponse(Item item);

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "myRole", source = "permission.role")
    ItemListResponse toListResponse(Item item, ItemPermission itemPermission);

    ItemUpdateResponse toUpdateResponse(Item item);

}
