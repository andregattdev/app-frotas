package com.andregattermeyer.api_frota.auth;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.andregattermeyer.api_frota.auth.dto.AuthenticationResponse;
import com.andregattermeyer.api_frota.auth.dto.AlterarSenhaRequest;
import com.andregattermeyer.api_frota.auth.dto.LoginRequest;
import com.andregattermeyer.api_frota.auth.dto.RegistrarRequest;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registrar")
    public ResponseEntity<AuthenticationResponse> registrar(@RequestBody @Valid RegistrarRequest request) {
        return ResponseEntity.ok(authService.registrar(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/alterar-senha")
    public ResponseEntity<Void> alterarSenha(
            @RequestBody @Valid AlterarSenhaRequest request,
            @AuthenticationPrincipal UserDetails usuario) {
        authService.alterarSenha(usuario.getUsername(), request);
        return ResponseEntity.noContent().build();
    }
}
