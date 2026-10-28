package com.andregattermeyer.api_frota.auth.dto;

import java.util.Set;
import java.util.stream.Collectors;

import com.andregattermeyer.api_frota.auth.Usuario;

public class AuthenticationResponse {

    private final String token;
    private final String username;
    private final Set<String> roles;

    public AuthenticationResponse(String token, String username, Set<String> roles) {
        this.token = token;
        this.username = username;
        this.roles = Set.copyOf(roles);
    }

    public static AuthenticationResponse from(Usuario usuario, String token) {
        Set<String> roles = usuario.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
        return new AuthenticationResponse(token, usuario.getUsername(), roles);
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public Set<String> getRoles() {
        return roles;
    }
}
