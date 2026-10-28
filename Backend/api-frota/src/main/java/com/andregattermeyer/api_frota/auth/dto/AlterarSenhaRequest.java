package com.andregattermeyer.api_frota.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AlterarSenhaRequest {

    @NotBlank
    private String senhaAtual;

    @NotBlank
    @Size(min = 12, max = 72)
    private String novaSenha;

    public String getSenhaAtual() {
        return senhaAtual;
    }

    public void setSenhaAtual(String senhaAtual) {
        this.senhaAtual = senhaAtual;
    }

    public String getNovaSenha() {
        return novaSenha;
    }

    public void setNovaSenha(String novaSenha) {
        this.novaSenha = novaSenha;
    }
}
