package com.andregattermeyer.api_frota.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.andregattermeyer.api_frota.entity.PontoParada;

public interface PontoParadaRepository extends JpaRepository<PontoParada, Long> {

    List<PontoParada> findByVeiculoIdAndRotaDiariaIsNullOrderByOrdemAscIdAsc(Long veiculoId);

    List<PontoParada> findByRotaDiaria_IdOrderByOrdemAscIdAsc(Long rotaDiariaId);

    Optional<PontoParada> findByIdAndVeiculoIdAndRotaDiariaIsNull(Long id, Long veiculoId);
}
