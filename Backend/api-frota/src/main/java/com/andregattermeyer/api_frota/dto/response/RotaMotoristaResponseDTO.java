package com.andregattermeyer.api_frota.dto.response;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

import com.andregattermeyer.api_frota.entity.StatusRotaDiaria;

public record RotaMotoristaResponseDTO(
        Long rotaId,
        StatusRotaDiaria status,
        LocalDate dataRota,
        LocalDateTime finalizadaEm,
        VeiculoResponseDTO veiculo,
        LocalDateTime atribuidaEm,
        List<PontoParadaResponseDTO> paradas,
        List<RegistroParadaResponseDTO> registros) {
}
