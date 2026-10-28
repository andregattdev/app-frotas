package com.andregattermeyer.api_frota.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OficinaRequestDTO {

    @NotBlank(message = "O nome da oficina é obrigatório")
    private String nomeOficina;

    @NotBlank(message = "O telefone da oficina é obrigatório")
    private String telefone;

    @NotBlank(message = "A especialidade da oficina é obrigatória")
    private String especialidade; // ex: mecânica geral, elétrica, pneus

    @Valid
    @NotNull(message = "O endereço da oficina é obrigatório")
    private EnderecoRequestDTO endereco;
    


    public String getNomeOficina() {
        return this.nomeOficina;
    }

    public void setNomeOficina(String nomeOficina) {
        this.nomeOficina = nomeOficina;
    }

    public String getTelefone() {
        return this.telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEspecialidade() {
        return this.especialidade;
    }

    public void setEspecialidade(String especialidade) {
        this.especialidade = especialidade;
    }

    public EnderecoRequestDTO getEndereco() {
        return this.endereco;
    }

    public void setEndereco(EnderecoRequestDTO endereco) {
        this.endereco = endereco;
    }


}
