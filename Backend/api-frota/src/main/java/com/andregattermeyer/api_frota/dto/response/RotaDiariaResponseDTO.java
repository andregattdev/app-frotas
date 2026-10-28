package com.andregattermeyer.api_frota.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.andregattermeyer.api_frota.entity.RotaDiaria;
import com.andregattermeyer.api_frota.entity.StatusRotaDiaria;

public record RotaDiariaResponseDTO(
        Long id,
        LocalDate dataRota,
        StatusRotaDiaria status,
        LocalDateTime criadaEm,
        LocalDateTime finalizadaEm,
        Long motoristaId,
        String motoristaNome,
        Long veiculoId,
        String veiculoPlaca,
        List<PontoParadaResponseDTO> paradas,
        List<RegistroParadaResponseDTO> registros) {

    public static RotaDiariaResponseDTO from(
            RotaDiaria rota,
            List<PontoParadaResponseDTO> paradas,
            List<RegistroParadaResponseDTO> registros) {
        return new RotaDiariaResponseDTO(
                rota.getId(),
                rota.getDataRota(),
                rota.getStatus(),
                rota.getCriadaEm(),
                rota.getFinalizadaEm(),
                rota.getMotorista().getId(),
                rota.getMotorista().getNome(),
                rota.getVeiculo().getId(),
                rota.getVeiculo().getPlaca(),
                paradas,
                registros);
    }
}
