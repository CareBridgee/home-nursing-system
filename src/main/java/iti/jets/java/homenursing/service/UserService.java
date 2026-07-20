package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.UserResponse;
import iti.jets.java.homenursing.dto.UserUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponse getCurrentUser(UUID userId);

    UserResponse getById(UUID userId);

    UserResponse updateCurrentUser(UUID userId, UserUpdateRequest request);

    void deleteCurrentUser(UUID userId);

    Page<UserResponse> listUsers(Pageable pageable);
}
