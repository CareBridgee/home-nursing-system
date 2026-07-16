package iti.jets.java.carenest.controller;

import iti.jets.java.carenest.dto.LoginRequest;
import iti.jets.java.carenest.dto.RefreshRequest;
import iti.jets.java.carenest.dto.TokenPair;
import iti.jets.java.carenest.dto.UserResponse;
import iti.jets.java.carenest.dto.VerifyOtpRequest;
import iti.jets.java.carenest.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<Void> sendOtp(@Valid @RequestBody LoginRequest request) {
        authService.requestOtp(request.getPhoneNumber());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<TokenPair> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        TokenPair tokens = authService.verifyOtpAndLogin(
                request.getPhoneNumber(), request.getOtp());
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refreshToken(
            @Valid @RequestBody RefreshRequest request) {
        TokenPair tokens = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getUserProfile(@RequestParam String phoneNumber) {
        return ResponseEntity.ok(authService.getUserProfile(phoneNumber));
    }
}
