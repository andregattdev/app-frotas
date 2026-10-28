package com.andregattermeyer.api_frota.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarEncaminhamentoManutencaoRequest(
        @NotNull Long veiculoId,
        @NotNull Long oficinaId,
        @Size(max = 1000) String orientacao) {
}
