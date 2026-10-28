package com.andregattermeyer.api_frota.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.andregattermeyer.api_frota.entity.EncaminhamentoManutencao;

public interface EncaminhamentoManutencaoRepository extends JpaRepository<EncaminhamentoManutencao, Long> {

    List<EncaminhamentoManutencao> findByAtivoTrueOrderByEnviadoEmDesc();

    List<EncaminhamentoManutencao> findByMotoristaIdAndAtivoTrueOrderByEnviadoEmDesc(Long motoristaId);

    Optional<EncaminhamentoManutencao> findByIdAndAtivoTrue(Long id);

    Optional<EncaminhamentoManutencao> findByVeiculoIdAndAtivoTrue(Long veiculoId);
}
