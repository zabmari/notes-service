package com.marika.notesservice.mapper;

import com.marika.notesservice.dto.item.CreateItemRequest;
import com.marika.notesservice.dto.item.ItemListResponse;
import com.marika.notesservice.dto.item.ItemResponse;
import com.marika.notesservice.dto.item.ItemUpdateResponse;
import com.marika.notesservice.model.Item;
import com.marika.notesservice.model.ItemPermission;
import com.marika.notesservice.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @org.mapstruct.Builder(disableBuilder = false))
public interface ItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Item toEntity(CreateItemRequest createItemRequest, User user);

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    ItemResponse toResponse(Item item);

    @Mapping(target = "id", source = "item.id")
    @Mapping(target = "ownerId",
            expression = "java(item.getOwner() != null ? item.getOwner().getId() : null)")
    @Mapping(target = "myRole", source = "itemPermission.role")
    @Mapping(target = "updatedAt", source = "item.updatedAt")
    ItemListResponse toListResponse(Item item, ItemPermission itemPermission);

    ItemUpdateResponse toUpdateResponse(Item item);
}
