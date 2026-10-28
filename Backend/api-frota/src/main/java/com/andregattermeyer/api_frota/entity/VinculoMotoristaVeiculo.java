package com.andregattermeyer.api_frota.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_vinculo_motorista_veiculo")
public class VinculoMotoristaVeiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private Veiculo veiculo;

    @ManyToOne
    private Motorista motorista;

    private LocalDateTime dataHoraInicio;
    private LocalDateTime dataHoraFim;
    private boolean ativo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Veiculo getVeiculo() { return veiculo; }
    public void setVeiculo(Veiculo veiculo) { this.veiculo = veiculo; }
    public Motorista getMotorista() { return motorista; }
    public void setMotorista(Motorista motorista) { this.motorista = motorista; }
    public LocalDateTime getDataHoraInicio() { return dataHoraInicio; }
    public void setDataHoraInicio(LocalDateTime dataHoraInicio) { this.dataHoraInicio = dataHoraInicio; }
    public LocalDateTime getDataHoraFim() { return dataHoraFim; }
    public void setDataHoraFim(LocalDateTime dataHoraFim) { this.dataHoraFim = dataHoraFim; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}