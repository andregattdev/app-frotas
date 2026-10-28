package com.andregattermeyer.api_frota.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record RotaDiariaRequestDTO(
        @NotNull Long motoristaId,
        @NotNull Long veiculoId,
        @NotNull LocalDate dataRota,
        @NotEmpty List<@Valid ParadaRotaDiariaRequestDTO> paradas) {
}
