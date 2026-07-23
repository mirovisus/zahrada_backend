package upce.fei.garden.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import upce.fei.garden.dto.auth.AuthResponse;
import upce.fei.garden.dto.auth.LoginRequest;
import upce.fei.garden.dto.auth.RegisterRequest;
import upce.fei.garden.service.AuthService;

/**
 * Veřejné endpointy pro registraci a přihlášení – jediné, které nevyžadují JWT (viz {@code SecurityConfig}).
 */
@Tag(name = "Autentizace", description = "Registrace a přihlášení uživatelů")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Registrace nového uživatele (vlastník zahrady nebo zahradník)")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(summary = "Přihlášení existujícího uživatele")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
