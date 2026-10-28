package com.andregattermeyer.api_frota.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.andregattermeyer.api_frota.entity.Checklist;

public interface ChecklistRepository extends JpaRepository<Checklist, Long> {

    // Checklists de um veículo
    List<Checklist> findByVeiculoId(Long veiculoId);

    // Checklists feitos por um motorista
    List<Checklist> findByMotoristaId(Long motoristaId);

    // Checklists de um veículo em um período
    @Query("SELECT c FROM Checklist c WHERE c.veiculo.id = :veiculoId AND c.dataHora BETWEEN :inicio AND :fim")
    List<Checklist> findByVeiculoIdAndPeriodo(
            @Param("veiculoId") Long veiculoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    // Último checklist de um veículo
    @Query("SELECT c FROM Checklist c WHERE c.veiculo.id = :veiculoId ORDER BY c.dataHora DESC LIMIT 1")
    Checklist findUltimoByVeiculoId(@Param("veiculoId") Long veiculoId);
}