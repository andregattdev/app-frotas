package com.andregattermeyer.api_frota.dto.request;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class MotoristaRequestDTO {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    private String nome;

    @NotBlank(message = "CNH é obrigatória")
    @Size(min = 11, max = 11, message = "CNH deve ter 11 caracteres")
    private String cnh;

    @NotBlank(message = "Categoria é obrigatória")
    private String categoria;

    @NotNull(message = "Validade da CNH é obrigatória")
    private LocalDate validadeCnh;

    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    private String telefone;

    @Valid
    private EnderecoRequestDTO endereco;

    
    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCnh() {
        return cnh;
    }

    public void setCnh(String cnh) {
        this.cnh = cnh;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public LocalDate getValidadeCnh() {
        return validadeCnh;
    }

    public void setValidadeCnh(LocalDate validadeCnh) {
        this.validadeCnh = validadeCnh;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public EnderecoRequestDTO getEndereco() {
        return endereco;
    }

    public void setEndereco(EnderecoRequestDTO endereco) {
        this.endereco = endereco;
    }
}