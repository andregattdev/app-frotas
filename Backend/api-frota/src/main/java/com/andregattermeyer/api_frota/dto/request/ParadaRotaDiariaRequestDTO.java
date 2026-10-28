package com.andregattermeyer.api_frota.dto.request;

import com.andregattermeyer.api_frota.entity.TipoParada;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record ParadaRotaDiariaRequestDTO(
        @NotBlank @Size(max = 255) String descricao,
        @Size(max = 255) String nomeEstabelecimento,
        @Size(max = 500) String endereco,
        @NotNull TipoParada tipo,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude) {
}
