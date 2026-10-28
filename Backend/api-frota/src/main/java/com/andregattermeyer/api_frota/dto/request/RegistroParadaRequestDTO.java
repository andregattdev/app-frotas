package com.andregattermeyer.api_frota.dto.request;

import com.andregattermeyer.api_frota.entity.TipoRegistroParada;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistroParadaRequestDTO(
        @NotNull TipoRegistroParada tipo,
        @Size(max = 1000) String observacao,
        @NotNull UUID clientRequestId) {
}