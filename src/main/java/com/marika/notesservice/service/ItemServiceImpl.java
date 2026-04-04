package com.marika.notesservice.service;

import com.marika.notesservice.audit.CustomRevisionEntity;
import com.marika.notesservice.dto.item.CreateItemRequest;
import com.marika.notesservice.dto.item.ItemHistoryResponse;
import com.marika.notesservice.dto.item.ItemListResponse;
import com.marika.notesservice.dto.item.ItemResponse;
import com.marika.notesservice.dto.item.ItemUpdateRequest;
import com.marika.notesservice.dto.item.ItemUpdateResponse;
import com.marika.notesservice.dto.item.ShareRequest;
import com.marika.notesservice.dto.item.ShareResponse;
import com.marika.notesservice.exception.ResourceNotFoundException;
import com.marika.notesservice.exception.SelfShareException;
import com.marika.notesservice.exception.VersionConflictException;
import com.marika.notesservice.mapper.ItemMapper;
import com.marika.notesservice.model.Item;
import com.marika.notesservice.model.ItemPermission;
import com.marika.notesservice.model.User;
import com.marika.notesservice.model.enums.PermissionRole;
import com.marika.notesservice.repository.ItemPermissionRepository;
import com.marika.notesservice.repository.ItemRepository;
import com.marika.notesservice.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ItemMapper itemMapper;
    private final ItemPermissionRepository itemPermissionRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private User getCurrentUser() {
        String login = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByLogin(login)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Item loadItem(UUID id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));
    }

    private ItemPermission getPermissionOrThrow(Item item, User user) {
        return itemPermissionRepository
                .findByItemAndUser(item, user)
                .orElseThrow(() -> new AccessDeniedException("No permission"));
    }

    private void assertOwner(ItemPermission permission) {
        if (permission.getRole() != PermissionRole.OWNER) {
            throw new AccessDeniedException("Only owner can manage permissions");
        }
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ItemPermission validateAccess(UUID itemId, User user) {
        return itemPermissionRepository.findByItemIdAndUserId(itemId, user.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "Forbidden: You don't have access to this item"));
    }

    @Transactional
    public ItemResponse createItem(CreateItemRequest createItemRequest) {
        User currentUser = getCurrentUser();

        Item item = itemMapper.toEntity(createItemRequest, currentUser);
        item.setOwner(currentUser);
        item.setDeleted(false);
        item = itemRepository.saveAndFlush(item);

        ItemPermission itemPermission = new ItemPermission();
        itemPermission.setItem(item);
        itemPermission.setUser(currentUser);
        itemPermission.setRole(PermissionRole.OWNER);

        itemPermissionRepository.saveAndFlush(itemPermission);
        return itemMapper.toResponse(item);
    }

    @Transactional(readOnly = true)
    public List<ItemListResponse> getItemsForCurrentUser() {
        User currentUser = getCurrentUser();

        return itemPermissionRepository.findByUser(currentUser).stream()
                .map(permission -> itemMapper.toListResponse(permission.getItem(), permission))
                .toList();
    }

    @Transactional
    public ItemUpdateResponse updateItem(UUID id, ItemUpdateRequest updateItemRequest) {
        User currentUser = getCurrentUser();
        ItemPermission itemPermission = validateAccess(id, currentUser);
        Item item = loadItem(id);

        if (itemPermission.getRole() == PermissionRole.VIEWER) {
            throw new AccessDeniedException("No permission to edit this item");
        }

        if (!item.getVersion().equals(updateItemRequest.version())) {
            throw new VersionConflictException(item.getVersion());
        }

        if (updateItemRequest.title() != null) {
            item.setTitle(updateItemRequest.title());
        }
        if (updateItemRequest.content() != null) {
            item.setContent(updateItemRequest.content());
        }

        Item updatedItem = itemRepository.saveAndFlush(item);

        return itemMapper.toUpdateResponse(updatedItem);
    }

    @Transactional
    public void softDeleteItem(UUID id) {
        User currentUser = getCurrentUser();
        ItemPermission itemPermission = validateAccess(id, currentUser);
        Item item = loadItem(id);
        assertOwner(itemPermission);

        item.setDeleted(true);
        itemRepository.saveAndFlush(item);
        itemPermissionRepository.deleteByItem(item);
    }

    @Transactional(readOnly = true)
    public List<ItemHistoryResponse> getItemHistory(UUID id) {
        User currentUser = getCurrentUser();
        Item item = loadItem(id);

        if (item.getDeleted()) {
            throw new ResourceNotFoundException("Item not found");
        }

        validateAccess(id, currentUser);

        AuditReader auditReader = AuditReaderFactory.get(entityManager);

        List<Object[]> revisions = auditReader.createQuery()
                .forRevisionsOfEntity(Item.class, false, true)
                .add(AuditEntity.id().eq(id))
                .getResultList();

        return revisions.stream()
                .map(row -> {
                    Item itemRevision = (Item) row[0];
                    CustomRevisionEntity rev = (CustomRevisionEntity) row[1];
                    RevisionType type = (RevisionType) row[2];

                    return new ItemHistoryResponse(
                            rev.getId(),
                            type.name(),
                            Instant.ofEpochMilli(rev.getTimestamp()),
                            rev.getChangedBy(),
                            itemRevision.getTitle(),
                            itemRevision.getContent()
                    );
                })
                .toList();
    }

    @Transactional
    public ShareResponse shareItem(UUID id, ShareRequest shareRequest) {
        User currentUser = getCurrentUser();
        ItemPermission itemPermission = validateAccess(id, currentUser);

        assertOwner(itemPermission);
        User targetUser = loadUser(shareRequest.userId());

        if (targetUser.getId().equals(currentUser.getId())) {
            throw new SelfShareException("Cannot grant access to yourself");
        }

        PermissionRole role;
        try {
            role = PermissionRole.valueOf(shareRequest.role());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid role: " + shareRequest.role());
        }

        if (role == PermissionRole.OWNER) {
            throw new IllegalArgumentException("Cannot assign OWNER role");
        }

        Item item = loadItem(id);

        ItemPermission targetPermission = itemPermissionRepository
                .findByItemAndUser(item, targetUser)
                .orElse(null);

        boolean isNew = false;

        if (targetPermission == null) {
            isNew = true;
            targetPermission = new ItemPermission();
            targetPermission.setItem(item);
            targetPermission.setUser(targetUser);
        }

        targetPermission.setRole(role);
        itemPermissionRepository.save(targetPermission);

        return new ShareResponse(
                item.getId(),
                targetUser.getId(),
                role.name(),
                Instant.now(),
                isNew
        );
    }

    @Transactional
    public void revokeShare(UUID itemId, UUID targetUserId) {
        User currentUser = getCurrentUser();
        ItemPermission itemPermission = validateAccess(itemId, currentUser);
        Item item = loadItem(itemId);

        assertOwner(itemPermission);
        User targetUser = loadUser(targetUserId);

        ItemPermission targetPermission = itemPermissionRepository
                .findByItemAndUser(item, targetUser)
                .orElseThrow(
                        () -> new ResourceNotFoundException("User has no access to this item"));

        itemPermissionRepository.delete(targetPermission);
    }
}
