package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.*;
import iti.jets.java.homenursing.service.AuthService;
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

    @PostMapping("/nurse/login")
    public ResponseEntity<Void> sendNurseOtp(@Valid @RequestBody LoginRequest request) {
        authService.requestOtp(request.getPhoneNumber());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/nurse/verify-otp")
    public ResponseEntity<TokenPair> verifyNurseOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        TokenPair tokens = authService.verifyNurseOtpAndLogin(
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
