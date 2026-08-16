package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.annotation.SortableFields;
import iti.jets.java.homenursing.dto.user.*;
import iti.jets.java.homenursing.security.SecurityUtils;
import iti.jets.java.homenursing.service.UserService;
import iti.jets.java.homenursing.util.SortSanitizer;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser(SecurityUtils.currentUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> updateCurrentUser(@Valid @ModelAttribute UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateCurrentUser(SecurityUtils.currentUserId(), request));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser() {
        userService.deleteCurrentUser(SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @SortableFields(SortSanitizer.USER_SORTABLE)
    public ResponseEntity<Page<UserResponse>> listUsers(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(userService.listUsers(pageable));
    }

    @PatchMapping("/{userId}/credit")
    public ResponseEntity<CreditUpdateResponse> updateCredit(
            @PathVariable UUID userId,
            @Valid @RequestBody CreditUpdateRequest request) {

        return ResponseEntity.ok(
                userService.updateCredit(userId, request)
        );
    }
    @GetMapping("/{userId}/credit")
    public ResponseEntity<CreditResponse> getCredit(
            @PathVariable UUID userId) {

        return ResponseEntity.ok(
                userService.getCredit(userId)
        );
    }
}
