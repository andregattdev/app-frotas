package com.andregattermeyer.api_frota.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.andregattermeyer.api_frota.entity.Motorista;

public interface MotoristaRepository extends JpaRepository<Motorista, Long>, JpaSpecificationExecutor<Motorista> {

    // Buscar por nome exato
    List<Motorista> findByNome(String nome);

    // Buscar ignorando maiúsculas/minúsculas
    List<Motorista> findByNomeIgnoreCase(String nome);

    // Buscar por parte do nome (like %nome%)
    List<Motorista> findByNomeContainingIgnoreCase(String nome);

    // Buscar por CNH
    Optional<Motorista> findByCnh(String cnh);

    // Buscar por categoria da CNH
    List<Motorista> findByCategoria(String categoria);

    // Buscar motoristas com CNH vencida
    @Query("SELECT m FROM Motorista m WHERE m.validadeCnh < CURRENT_DATE")
    List<Motorista> findMotoristasComCnhVencida();
   
}
