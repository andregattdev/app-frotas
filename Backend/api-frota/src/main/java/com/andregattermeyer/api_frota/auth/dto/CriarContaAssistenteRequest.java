package com.andregattermeyer.api_frota.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CriarContaAssistenteRequest {

    @NotBlank
    @Size(max = 100)
    private String username;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(min = 12, max = 72)
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
