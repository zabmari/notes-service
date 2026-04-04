package com.marika.notesservice.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marika.notesservice.dto.item.ItemUpdateRequest;
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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ItemServiceImplTest {
    @Mock
    ItemMapper itemMapper;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemPermissionRepository itemPermissionRepository;
    @InjectMocks
    private ItemServiceImpl itemService;

    private User currentUser;
    private Item item;

    @BeforeEach
    void setup() {
        currentUser = new User();
        currentUser.setId(UUID.randomUUID());
        currentUser.setLogin("test_user");
        mockCurrentUser(currentUser);

        item = new Item();
        item.setId(UUID.randomUUID());
        item.setOwner(currentUser);
        item.setVersion(1);
        item.setDeleted(false);
    }

    private void mockCurrentUser(User user) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(user.getLogin());

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByLogin(user.getLogin())).thenReturn(Optional.of(user));
    }

    @Test
    void ownerCanEditItem() {

        ItemPermission permission = new ItemPermission();
        permission.setItem(item);
        permission.setUser(currentUser);
        permission.setRole(PermissionRole.OWNER);

        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(itemPermissionRepository.findByItemIdAndUserId(item.getId(),
                currentUser.getId())).thenReturn(
                Optional.of(permission));

        ItemUpdateRequest request = new ItemUpdateRequest("new title", "new content", 1);

        assertDoesNotThrow(() -> itemService.updateItem(item.getId(), request));
    }

    @Test
    void editorCanEditItem() {

        ItemPermission permission = new ItemPermission();
        permission.setItem(item);
        permission.setUser(currentUser);
        permission.setRole(PermissionRole.EDITOR);

        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(itemPermissionRepository.findByItemIdAndUserId(item.getId(),
                currentUser.getId())).thenReturn(
                Optional.of(permission));

        ItemUpdateRequest request = new ItemUpdateRequest("new title", "new content", 1);

        assertDoesNotThrow(() -> itemService.updateItem(item.getId(), request));
    }

    @Test
    void viewerCannotEditItem() {

        ItemPermission permission = new ItemPermission();
        permission.setItem(item);
        permission.setUser(currentUser);
        permission.setRole(PermissionRole.VIEWER);

        when(itemPermissionRepository.findByItemIdAndUserId(item.getId(),
                currentUser.getId())).thenReturn(
                Optional.of(permission));

        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        ItemUpdateRequest request = new ItemUpdateRequest("new title", "new content", 1);

        assertThrows(AccessDeniedException.class,
                () -> itemService.updateItem(item.getId(), request));
    }

    @Test
    void strangerCannotEditItem() {
        when(itemPermissionRepository.findByItemIdAndUserId(item.getId(),
                currentUser.getId())).thenReturn(
                Optional.empty());

        ItemUpdateRequest request = new ItemUpdateRequest("new title", "new content", 1);

        assertThrows(AccessDeniedException.class,
                () -> itemService.updateItem(item.getId(), request));
    }

    @Test
    void updateItemThrowsVersionConflict() {

        ItemPermission permission = new ItemPermission();
        permission.setRole(PermissionRole.EDITOR);


        when(itemPermissionRepository.findByItemIdAndUserId(item.getId(),
                currentUser.getId())).thenReturn(
                Optional.of(permission));

        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        ItemUpdateRequest request = new ItemUpdateRequest("new title", "new content", 3);

        VersionConflictException ex = assertThrows(VersionConflictException.class,
                () -> itemService.updateItem(item.getId(), request));

        assertEquals(1, ex.getCurrentVersion());
    }

    @Test
    void createsNewPermissionIfNotExist() {
        ItemPermission ownerPerm = new ItemPermission();
        ownerPerm.setRole(PermissionRole.OWNER);

        when(itemPermissionRepository.findByItemIdAndUserId(item.getId(),
                currentUser.getId())).thenReturn(
                Optional.of(ownerPerm));

        User target = new User();
        target.setId(UUID.randomUUID());
        target.setLogin("target_user");

        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        when(itemPermissionRepository.findByItemAndUser(item, target)).thenReturn(Optional.empty());

        ShareRequest request = new ShareRequest(target.getId(), "EDITOR");

        assertDoesNotThrow(() -> itemService.shareItem(item.getId(), request));
        verify(itemPermissionRepository, times(1)).save(any(ItemPermission.class));
    }

    @Test
    void updatesExistingPermissionRole() {
        ItemPermission ownerPerm = new ItemPermission();
        ownerPerm.setRole(PermissionRole.OWNER);
        when(itemPermissionRepository.findByItemIdAndUserId(item.getId(),
                currentUser.getId())).thenReturn(
                Optional.of(ownerPerm));

        User target = new User();
        target.setId(UUID.randomUUID());
        target.setLogin("target_user");

        ItemPermission existingPerm = new ItemPermission();
        existingPerm.setUser(target);
        existingPerm.setItem(item);
        existingPerm.setRole(PermissionRole.VIEWER);


        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        when(itemPermissionRepository.findByItemAndUser(item, target)).thenReturn(
                Optional.of(existingPerm));

        ShareRequest request = new ShareRequest(target.getId(), "EDITOR");

        ShareResponse response = itemService.shareItem(item.getId(), request);

        assertEquals("EDITOR", response.role());
        assertFalse(response.isNew());
        verify(itemPermissionRepository, times(1)).save(existingPerm);
    }

    @Test
    void cannotShareItemWithYourself() {
        ItemPermission ownerPerm = new ItemPermission();
        ownerPerm.setRole(PermissionRole.OWNER);
        when(itemPermissionRepository.findByItemIdAndUserId(item.getId(),
                currentUser.getId())).thenReturn(
                Optional.of(ownerPerm));

        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));

        ShareRequest request = new ShareRequest(currentUser.getId(), "EDITOR");

        assertThrows(SelfShareException.class, () -> itemService.shareItem(item.getId(), request));

    }

    @Test
    void cannotShareOwnerRole() {
        ItemPermission ownerPerm = new ItemPermission();
        ownerPerm.setRole(PermissionRole.OWNER);
        when(itemPermissionRepository.findByItemIdAndUserId(item.getId(),
                currentUser.getId())).thenReturn(
                Optional.of(ownerPerm));

        User target = new User();
        target.setId(UUID.randomUUID());
        target.setLogin("target_user");
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        ShareRequest request = new ShareRequest(target.getId(), "OWNER");

        assertThrows(IllegalArgumentException.class,
                () -> itemService.shareItem(item.getId(), request));
    }

    @Test
    void cannotShareWithNonExistingUser() {
        ItemPermission ownerPerm = new ItemPermission();
        ownerPerm.setRole(PermissionRole.OWNER);
        when(itemPermissionRepository.findByItemIdAndUserId(item.getId(),
                currentUser.getId())).thenReturn(
                Optional.of(ownerPerm));

        UUID nonExistingUserId = UUID.randomUUID();
        when(userRepository.findById(nonExistingUserId)).thenReturn(Optional.empty());

        ShareRequest request = new ShareRequest(nonExistingUserId, "EDITOR");

        assertThrows(ResourceNotFoundException.class,
                () -> itemService.shareItem(item.getId(), request));
    }

    @Test
    void cannotShareDeletedItem() {
        item.setDeleted(true);

        ItemPermission itemPermission = new ItemPermission();
        itemPermission.setRole(PermissionRole.OWNER);

        when(itemPermissionRepository.findByItemIdAndUserId(item.getId(), currentUser.getId()))
                .thenReturn(Optional.of(itemPermission));

        ShareRequest request = new ShareRequest(UUID.randomUUID(), "EDITOR");

        assertThrows(ResourceNotFoundException.class,
                () -> itemService.shareItem(item.getId(), request));
    }
}
