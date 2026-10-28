package com.andregattermeyer.api_frota.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
@Table(name = "rotas_diarias")
public class RotaDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "motorista_id", nullable = false)
    private Motorista motorista;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    @Column(name = "data_rota", nullable = false)
    private LocalDate dataRota;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusRotaDiaria status = StatusRotaDiaria.PLANEJADA;

    @Column(nullable = false)
    private LocalDateTime criadaEm;

    private LocalDateTime finalizadaEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Motorista getMotorista() { return motorista; }
    public void setMotorista(Motorista motorista) { this.motorista = motorista; }
    public Veiculo getVeiculo() { return veiculo; }
    public void setVeiculo(Veiculo veiculo) { this.veiculo = veiculo; }
    public LocalDate getDataRota() { return dataRota; }
    public void setDataRota(LocalDate dataRota) { this.dataRota = dataRota; }
    public StatusRotaDiaria getStatus() { return status; }
    public void setStatus(StatusRotaDiaria status) { this.status = status; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public void setCriadaEm(LocalDateTime criadaEm) { this.criadaEm = criadaEm; }
    public LocalDateTime getFinalizadaEm() { return finalizadaEm; }
    public void setFinalizadaEm(LocalDateTime finalizadaEm) { this.finalizadaEm = finalizadaEm; }
}
