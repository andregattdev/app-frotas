package com.andregattermeyer.api_frota.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "tb_encaminhamento_manutencao")
public class EncaminhamentoManutencao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "motorista_id", nullable = false)
    private Motorista motorista;

    private Long oficinaId;
    private String oficinaNome;
    private String oficinaTelefone;
    private String oficinaEspecialidade;
    private String oficinaLogradouro;
    private String oficinaNumero;
    private String oficinaComplemento;
    private String oficinaBairro;
    private String oficinaCidade;
    private String oficinaEstado;
    private String oficinaCep;

    @Column(length = 1000)
    private String orientacao;
    private LocalDateTime enviadoEm;
    private boolean ativo = true;

    public Long getId() {
        return id;
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

    public Long getOficinaId() {
        return oficinaId;
    }

    public void setOficinaId(Long oficinaId) {
        this.oficinaId = oficinaId;
    }

    public String getOficinaNome() {
        return oficinaNome;
    }

    public void setOficinaNome(String oficinaNome) {
        this.oficinaNome = oficinaNome;
    }

    public String getOficinaTelefone() {
        return oficinaTelefone;
    }

    public void setOficinaTelefone(String oficinaTelefone) {
        this.oficinaTelefone = oficinaTelefone;
    }

    public String getOficinaEspecialidade() {
        return oficinaEspecialidade;
    }

    public void setOficinaEspecialidade(String oficinaEspecialidade) {
        this.oficinaEspecialidade = oficinaEspecialidade;
    }

    public String getOficinaLogradouro() {
        return oficinaLogradouro;
    }

    public void setOficinaLogradouro(String oficinaLogradouro) {
        this.oficinaLogradouro = oficinaLogradouro;
    }

    public String getOficinaNumero() {
        return oficinaNumero;
    }

    public void setOficinaNumero(String oficinaNumero) {
        this.oficinaNumero = oficinaNumero;
    }

    public String getOficinaComplemento() {
        return oficinaComplemento;
    }

    public void setOficinaComplemento(String oficinaComplemento) {
        this.oficinaComplemento = oficinaComplemento;
    }

    public String getOficinaBairro() {
        return oficinaBairro;
    }

    public void setOficinaBairro(String oficinaBairro) {
        this.oficinaBairro = oficinaBairro;
    }

    public String getOficinaCidade() {
        return oficinaCidade;
    }

    public void setOficinaCidade(String oficinaCidade) {
        this.oficinaCidade = oficinaCidade;
    }

    public String getOficinaEstado() {
        return oficinaEstado;
    }

    public void setOficinaEstado(String oficinaEstado) {
        this.oficinaEstado = oficinaEstado;
    }

    public String getOficinaCep() {
        return oficinaCep;
    }

    public void setOficinaCep(String oficinaCep) {
        this.oficinaCep = oficinaCep;
    }

    public String getOrientacao() {
        return orientacao;
    }

    public void setOrientacao(String orientacao) {
        this.orientacao = orientacao;
    }

    public LocalDateTime getEnviadoEm() {
        return enviadoEm;
    }

    public void setEnviadoEm(LocalDateTime enviadoEm) {
        this.enviadoEm = enviadoEm;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
