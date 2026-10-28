package com.andregattermeyer.api_frota.dto.response;

import com.andregattermeyer.api_frota.entity.PontoParada;
import com.andregattermeyer.api_frota.entity.TipoParada;

public record PontoParadaResponseDTO(
        Long id,
        String descricao,
        String nomeEstabelecimento,
        String endereco,
        TipoParada tipo,
        Double latitude,
        Double longitude,
        Integer ordem) {

    public static PontoParadaResponseDTO from(PontoParada ponto) {
        return new PontoParadaResponseDTO(
                ponto.getId(),
                ponto.getDescricao(),
                ponto.getNomeEstabelecimento(),
                ponto.getEndereco(),
                ponto.getTipo(),
                ponto.getLatitude(),
                ponto.getLongitude(),
                ponto.getOrdem());
    }
}
