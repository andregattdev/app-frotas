package com.andregattermeyer.api_frota.auth.dto;

public record ContaMotoristaResponse(
        Long motoristaId,
        String motoristaNome,
        String username,
        String email,
        String role) {
}
