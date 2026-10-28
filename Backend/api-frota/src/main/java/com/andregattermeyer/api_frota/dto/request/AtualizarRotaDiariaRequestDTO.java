package com.andregattermeyer.api_frota.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record AtualizarRotaDiariaRequestDTO(
        @NotEmpty List<@Valid ParadaRotaDiariaAtualizacaoRequestDTO> paradas) {
}
