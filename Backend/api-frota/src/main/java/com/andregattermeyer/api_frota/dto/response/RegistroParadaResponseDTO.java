package com.andregattermeyer.api_frota.dto.response;

import java.time.LocalDateTime;

import com.andregattermeyer.api_frota.entity.RegistroParada;
import com.andregattermeyer.api_frota.entity.TipoParada;
import com.andregattermeyer.api_frota.entity.TipoRegistroParada;

public record RegistroParadaResponseDTO(
        Long id,
        Long paradaId,
        String paradaDescricao,
        String nomeEstabelecimento,
        String endereco,
        TipoParada tipoParada,
        Double latitude,
        Double longitude,
        TipoRegistroParada tipo,
        String observacao,
        boolean possuiFoto,
        Double latitudeRegistro,
        Double longitudeRegistro,
        Long motoristaId,
        String motoristaNome,
        String usuarioMotorista,
        Long veiculoId,
        String veiculoPlaca,
        LocalDateTime registradoEm) {

    public static RegistroParadaResponseDTO from(RegistroParada registro) {
        var parada = registro.getPontoParada();
        var veiculo = parada.getVeiculo();
        var usuario = registro.getUsuarioMotorista();
        var motorista = usuario.getMotorista();
        return new RegistroParadaResponseDTO(
                registro.getId(),
                parada.getId(),
                parada.getDescricao(),
                parada.getNomeEstabelecimento(),
                parada.getEndereco(),
                parada.getTipo(),
                parada.getLatitude(),
                parada.getLongitude(),
                registro.getTipo(),
                registro.getObservacao(),
                registro.getFotoNomeArquivo() != null,
                registro.getLatitudeRegistro(),
                registro.getLongitudeRegistro(),
                motorista.getId(),
                motorista.getNome(),
                usuario.getUsername(),
                veiculo.getId(),
                veiculo.getPlaca(),
                registro.getRegistradoEm());
    }
}