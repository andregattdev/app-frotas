package com.andregattermeyer.api_frota.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.andregattermeyer.api_frota.entity.VinculoMotoristaVeiculo;

public interface VinculoMotoristaVeiculoRepository extends JpaRepository<VinculoMotoristaVeiculo, Long> {
    List<VinculoMotoristaVeiculo> findByVeiculoIdAndAtivoTrue(Long veiculoId);
    List<VinculoMotoristaVeiculo> findByVeiculoIdOrderByDataHoraInicioDesc(Long veiculoId);
    List<VinculoMotoristaVeiculo> findByMotoristaIdAndAtivoTrueOrderByDataHoraInicioDesc(Long motoristaId);
    Optional<VinculoMotoristaVeiculo> findByVeiculoIdAndMotoristaIdAndAtivoTrue(Long veiculoId, Long motoristaId);
}