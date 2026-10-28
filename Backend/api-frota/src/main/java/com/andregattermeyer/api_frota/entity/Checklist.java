package com.andregattermeyer.api_frota.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_checklist")
public class Checklist {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private LocalDateTime dataHora;          // Data e hora da verificação

    private Integer quilometragem;           // KM no momento do checklist

    // Itens de verificação (true = OK, false = com problema)
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

    // Relacionamentos
    @ManyToOne
    @JoinColumn(name = "veiculo_id", nullable = false)
    @JsonIgnoreProperties({"motoristas", "checklists"})
    private Veiculo veiculo;

    @ManyToOne
    @JoinColumn(name = "motorista_id")
    @JsonIgnoreProperties({"veiculos", "endereco", "checklists"})
    private Motorista motorista;             // Quem realizou o checklist

    public Checklist() {
    }

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

    public Veiculo getVeiculo() {
        return veiculo;
    }

    public void setVeiculo(Veiculo veiculo) {
        this.veiculo = veiculo;
    }

    public Motorista getMotorista() {
        return motorista;
    }

    public void setMotorista(Motorista motorista) {
        this.motorista = motorista;
    }
}