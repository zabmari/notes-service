package com.marika.notesservice.service;

import com.marika.notesservice.dto.item.ItemUpdateRequest;
import com.marika.notesservice.dto.item.ShareRequest;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ItemServiceImplTest {
    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemPermissionRepository itemPermissionRepository;

    @Mock
    ItemMapper itemMapper;

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
        when(itemPermissionRepository.findByItemAndUser(item, currentUser)).thenReturn(Optional.of(permission));

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
        when(itemPermissionRepository.findByItemAndUser(item, currentUser)).thenReturn(Optional.of(permission));

        ItemUpdateRequest request = new ItemUpdateRequest("new title", "new content", 1);

        assertDoesNotThrow(() -> itemService.updateItem(item.getId(), request));
    }

    @Test
    void viewerCannotEditItem() {

        ItemPermission permission = new ItemPermission();
        permission.setItem(item);
        permission.setUser(currentUser);
        permission.setRole(PermissionRole.VIEWER);

        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(itemPermissionRepository.findByItemAndUser(item, currentUser)).thenReturn(Optional.of(permission));

        ItemUpdateRequest request = new ItemUpdateRequest("new title", "new content", 1);

        assertThrows(AccessDeniedException.class, () -> itemService.updateItem(item.getId(), request));
    }

    @Test
    void strangerCannotEditItem() {

        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(itemPermissionRepository.findByItemAndUser(item, currentUser)).thenReturn(Optional.empty());

        ItemUpdateRequest request = new ItemUpdateRequest("new title", "new content", 1);

        assertThrows(AccessDeniedException.class, () -> itemService.updateItem(item.getId(), request));
    }

    @Test
    void updateItemThrowsVersionConflict() {

        ItemPermission permission = new ItemPermission();
        permission.setItem(item);
        permission.setUser(currentUser);
        permission.setRole(PermissionRole.EDITOR);

        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(itemPermissionRepository.findByItemAndUser(item, currentUser)).thenReturn(Optional.of(permission));

        ItemUpdateRequest request = new ItemUpdateRequest("new title", "new content", 3);

        VersionConflictException ex = assertThrows(VersionConflictException.class, () -> itemService.updateItem(item.getId(), request));

        assertEquals(1, ex.getCurrentVersion());
    }

    @Test
    void createsNewPermissionIfNotExist() {

        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        ItemPermission ownerPerm = new ItemPermission();
        ownerPerm.setRole(PermissionRole.OWNER);
        when(itemPermissionRepository.findByItemAndUser(item, currentUser)).thenReturn(Optional.of(ownerPerm));

        User target = new User();
        target.setId(UUID.randomUUID());
        target.setLogin("target_user");
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        when(itemPermissionRepository.findByItemAndUser(item, target)).thenReturn(Optional.empty());

        ShareRequest request = new ShareRequest(target.getId(), "EDITOR");

        assertDoesNotThrow(() -> itemService.shareItem(item.getId(), request));
    }

    @Test
    void updatesExistingPermissionRole() {

        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        ItemPermission ownerPerm = new ItemPermission();
        ownerPerm.setRole(PermissionRole.OWNER);
        when(itemPermissionRepository.findByItemAndUser(item, currentUser)).thenReturn(Optional.of(ownerPerm));

        User target = new User();
        target.setId(UUID.randomUUID());
        target.setLogin("target_user");
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        ItemPermission existingPerm = new ItemPermission();
        existingPerm.setRole(PermissionRole.VIEWER);
        when(itemPermissionRepository.findByItemAndUser(item, target)).thenReturn(Optional.of(existingPerm));

        ShareRequest request = new ShareRequest(target.getId(), "EDITOR");

        itemService.shareItem(item.getId(), request);

        assertEquals(PermissionRole.EDITOR, existingPerm.getRole());
    }

    @Test
    void cannotShareItemWithYourself() {

        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        ItemPermission ownerPerm = new ItemPermission();
        ownerPerm.setRole(PermissionRole.OWNER);
        when(itemPermissionRepository.findByItemAndUser(item, currentUser)).thenReturn(Optional.of(ownerPerm));

        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));

        ShareRequest request = new ShareRequest(currentUser.getId(), "EDITOR");

        assertThrows(SelfShareException.class, () -> itemService.shareItem(item.getId(), request));

    }

    @Test
    void cannotShareOwnerRole() {
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        ItemPermission ownerPerm = new ItemPermission();
        ownerPerm.setRole(PermissionRole.OWNER);
        when(itemPermissionRepository.findByItemAndUser(item, currentUser)).thenReturn(Optional.of(ownerPerm));

        User target = new User();
        target.setId(UUID.randomUUID());
        target.setLogin("target_user");
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        ShareRequest request = new ShareRequest(target.getId(), "OWNER");

        assertThrows(ResponseStatusException.class, () -> itemService.shareItem(item.getId(), request));
    }

    @Test
    void cannotShareWithNonExistingUser() {

        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        ItemPermission ownerPerm = new ItemPermission();
        ownerPerm.setRole(PermissionRole.OWNER);
        when(itemPermissionRepository.findByItemAndUser(item, currentUser)).thenReturn(Optional.of(ownerPerm));

        UUID nonExistingUserId = UUID.randomUUID();
        when(userRepository.findById(nonExistingUserId)).thenReturn(Optional.empty());

        ShareRequest request = new ShareRequest(nonExistingUserId, "EDITOR");

        assertThrows(RuntimeException.class, () -> itemService.shareItem(item.getId(), request));
    }

    @Test
    void cannotShareDeletedItem() {
        item.setDeleted(true);

        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        ShareRequest request = new ShareRequest(UUID.randomUUID(), "EDITOR");

        assertThrows(ResourceNotFoundException.class, () -> itemService.shareItem(item.getId(), request));
    }
}
