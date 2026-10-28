package com.andregattermeyer.api_frota.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class ChecklistRequestDTO {

    private LocalDateTime dataHora;

    @PositiveOrZero(message = "Quilometragem não pode ser negativa")
    private Integer quilometragem;

    private Boolean pneus;
    private Boolean farois;
    private Boolean lanternas;
    private Boolean freios;
    private Boolean nivelOleo;
    private Boolean nivelAgua;
    private Boolean documentos;
    private Boolean extintor;
    private Boolean triangulo;
    private Boolean macaco;
    private Boolean estepe;
    private Boolean limpeza;

    private String observacoes;

    @NotNull(message = "Veículo é obrigatório")
    private Long veiculoId;

    private Long motoristaId;

    // Getters e Setters
    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public Integer getQuilometragem() {
        return quilometragem;
    }

    public void setQuilometragem(Integer quilometragem) {
        this.quilometragem = quilometragem;
    }

    public Boolean getPneus() {
        return pneus;
    }

    public void setPneus(Boolean pneus) {
        this.pneus = pneus;
    }

    public Boolean getFarois() {
        return farois;
    }

    public void setFarois(Boolean farois) {
        this.farois = farois;
    }

    public Boolean getLanternas() {
        return lanternas;
    }

    public void setLanternas(Boolean lanternas) {
        this.lanternas = lanternas;
    }

    public Boolean getFreios() {
        return freios;
    }

    public void setFreios(Boolean freios) {
        this.freios = freios;
    }

    public Boolean getNivelOleo() {
        return nivelOleo;
    }

    public void setNivelOleo(Boolean nivelOleo) {
        this.nivelOleo = nivelOleo;
    }

    public Boolean getNivelAgua() {
        return nivelAgua;
    }

    public void setNivelAgua(Boolean nivelAgua) {
        this.nivelAgua = nivelAgua;
    }

    public Boolean getDocumentos() {
        return documentos;
    }

    public void setDocumentos(Boolean documentos) {
        this.documentos = documentos;
    }

    public Boolean getExtintor() {
        return extintor;
    }

    public void setExtintor(Boolean extintor) {
        this.extintor = extintor;
    }

    public Boolean getTriangulo() {
        return triangulo;
    }

    public void setTriangulo(Boolean triangulo) {
        this.triangulo = triangulo;
    }

    public Boolean getMacaco() {
        return macaco;
    }

    public void setMacaco(Boolean macaco) {
        this.macaco = macaco;
    }

    public Boolean getEstepe() {
        return estepe;
    }

    public void setEstepe(Boolean estepe) {
        this.estepe = estepe;
    }

    public Boolean getLimpeza() {
        return limpeza;
    }

    public void setLimpeza(Boolean limpeza) {
        this.limpeza = limpeza;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public Long getVeiculoId() {
        return veiculoId;
    }

    public void setVeiculoId(Long veiculoId) {
        this.veiculoId = veiculoId;
    }

    public Long getMotoristaId() {
        return motoristaId;
    }

    public void setMotoristaId(Long motoristaId) {
        this.motoristaId = motoristaId;
    }
}