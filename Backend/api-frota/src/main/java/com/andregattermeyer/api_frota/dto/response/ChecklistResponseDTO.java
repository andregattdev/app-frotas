package com.andregattermeyer.api_frota.dto.response;

import java.time.LocalDateTime;

public class ChecklistResponseDTO {

    private Long id;
    private LocalDateTime dataHora;
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

    private VeiculoResponseDTO veiculo;
    private MotoristaResponseDTO motorista;

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public VeiculoResponseDTO getVeiculo() {
        return veiculo;
    }

    public void setVeiculo(VeiculoResponseDTO veiculo) {
        this.veiculo = veiculo;
    }

    public MotoristaResponseDTO getMotorista() {
        return motorista;
    }

    public void setMotorista(MotoristaResponseDTO motorista) {
        this.motorista = motorista;
    }
}