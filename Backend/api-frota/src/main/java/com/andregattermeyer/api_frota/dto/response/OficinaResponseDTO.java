package com.andregattermeyer.api_frota.dto.response;

public class OficinaResponseDTO {

    private Long id;

    private String nomeOficina;
    private String telefone;
    private String especialidade;
    private EnderecoResponseDTO endereco;


    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public EnderecoResponseDTO getEndereco() {
        return this.endereco;
    }

    public void setEndereco(EnderecoResponseDTO endereco) {
        this.endereco = endereco;
    }


}
