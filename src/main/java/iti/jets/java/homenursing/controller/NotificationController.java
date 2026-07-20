package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.notification.NotificationRequest;
import iti.jets.java.homenursing.dto.notification.NotificationResponse;
import iti.jets.java.homenursing.security.SecurityUtils;
import iti.jets.java.homenursing.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> create(@Valid @RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/notifications/" + response.id()))
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications() {
        return ResponseEntity.ok(notificationService.getMyNotifications(SecurityUtils.currentUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationService.getNotification(id, SecurityUtils.currentUserId()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationService.markRead(id, SecurityUtils.currentUserId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        notificationService.delete(id, SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
