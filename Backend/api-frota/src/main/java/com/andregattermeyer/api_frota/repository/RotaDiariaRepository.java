package com.andregattermeyer.api_frota.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.andregattermeyer.api_frota.entity.RotaDiaria;

public interface RotaDiariaRepository extends JpaRepository<RotaDiaria, Long> {
    List<RotaDiaria> findByMotorista_IdAndDataRotaGreaterThanEqualOrderByDataRotaAscIdAsc(
            Long motoristaId, LocalDate dataRota);
    Optional<RotaDiaria> findByMotorista_IdAndDataRota(Long motoristaId, LocalDate dataRota);
    List<RotaDiaria> findAllByOrderByDataRotaDescIdDesc();
    Optional<RotaDiaria> findByIdAndMotorista_Id(Long id, Long motoristaId);
    boolean existsByMotorista_IdAndDataRota(Long motoristaId, LocalDate dataRota);
    boolean existsByVeiculo_IdAndDataRota(Long veiculoId, LocalDate dataRota);
}
