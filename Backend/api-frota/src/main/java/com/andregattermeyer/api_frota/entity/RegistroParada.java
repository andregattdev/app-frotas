package com.andregattermeyer.api_frota.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.andregattermeyer.api_frota.auth.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "registros_parada")
public class RegistroParada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ponto_parada_id", nullable = false)
    private PontoParada pontoParada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_motorista_id", nullable = false)
    private Usuario usuarioMotorista;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoRegistroParada tipo;

    @Column(length = 1000)
    private String observacao;

    @Column(name = "foto_nome_arquivo", length = 80)
    private String fotoNomeArquivo;

    @Column(name = "latitude_registro")
    private Double latitudeRegistro;

    @Column(name = "longitude_registro")
    private Double longitudeRegistro;

    @Column(nullable = false)
    private LocalDateTime registradoEm;

    @Column(name = "client_request_id", unique = true)
    private UUID clientRequestId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PontoParada getPontoParada() { return pontoParada; }
    public void setPontoParada(PontoParada pontoParada) { this.pontoParada = pontoParada; }
    public Usuario getUsuarioMotorista() { return usuarioMotorista; }
    public void setUsuarioMotorista(Usuario usuarioMotorista) { this.usuarioMotorista = usuarioMotorista; }
    public TipoRegistroParada getTipo() { return tipo; }
    public void setTipo(TipoRegistroParada tipo) { this.tipo = tipo; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public String getFotoNomeArquivo() { return fotoNomeArquivo; }
    public void setFotoNomeArquivo(String fotoNomeArquivo) { this.fotoNomeArquivo = fotoNomeArquivo; }
    public Double getLatitudeRegistro() { return latitudeRegistro; }
    public void setLatitudeRegistro(Double latitudeRegistro) { this.latitudeRegistro = latitudeRegistro; }
    public Double getLongitudeRegistro() { return longitudeRegistro; }
    public void setLongitudeRegistro(Double longitudeRegistro) { this.longitudeRegistro = longitudeRegistro; }
    public LocalDateTime getRegistradoEm() { return registradoEm; }
    public void setRegistradoEm(LocalDateTime registradoEm) { this.registradoEm = registradoEm; }
    public UUID getClientRequestId() { return clientRequestId; }
    public void setClientRequestId(UUID clientRequestId) { this.clientRequestId = clientRequestId; }
}