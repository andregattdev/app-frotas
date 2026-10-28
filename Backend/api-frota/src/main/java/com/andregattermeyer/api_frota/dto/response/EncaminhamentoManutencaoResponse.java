package com.andregattermeyer.api_frota.dto.response;

import java.time.LocalDateTime;

public record EncaminhamentoManutencaoResponse(
        Long id,
        VeiculoResponseDTO veiculo,
        OficinaResponseDTO oficina,
        String orientacao,
        LocalDateTime enviadoEm) {
}
