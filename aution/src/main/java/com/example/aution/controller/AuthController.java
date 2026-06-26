package com.example.aution.controller;

import com.example.aution.dto.auth.AuthResponse;
import com.example.aution.dto.auth.LoginRequest;
import com.example.aution.dto.auth.RegisterRequest;
import com.example.aution.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * @Valid triggers RegisterRequest constraint annotations before the method runs.
     * If any field fails, MethodArgumentNotValidException is thrown automatically
     * and caught by GlobalExceptionHandler — returning 400 with fieldErrors map.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}