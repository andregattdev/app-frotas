package com.andregattermeyer.api_frota.dto.response;

import java.time.LocalDateTime;

public class VinculoMotoristaVeiculoResponseDTO {
    private Long id;
    private Long motoristaId;
    private String motoristaNome;
    private String motoristaCnh;
    private LocalDateTime dataHoraInicio;
    private LocalDateTime dataHoraFim;
    private boolean ativo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMotoristaId() { return motoristaId; }
    public void setMotoristaId(Long motoristaId) { this.motoristaId = motoristaId; }
    public String getMotoristaNome() { return motoristaNome; }
    public void setMotoristaNome(String motoristaNome) { this.motoristaNome = motoristaNome; }
    public String getMotoristaCnh() { return motoristaCnh; }
    public void setMotoristaCnh(String motoristaCnh) { this.motoristaCnh = motoristaCnh; }
    public LocalDateTime getDataHoraInicio() { return dataHoraInicio; }
    public void setDataHoraInicio(LocalDateTime dataHoraInicio) { this.dataHoraInicio = dataHoraInicio; }
    public LocalDateTime getDataHoraFim() { return dataHoraFim; }
    public void setDataHoraFim(LocalDateTime dataHoraFim) { this.dataHoraFim = dataHoraFim; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}