package com.andregattermeyer.api_frota.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.andregattermeyer.api_frota.entity.Oficina;

public interface OficinaRepository extends JpaRepository<Oficina, Long>, JpaSpecificationExecutor<Oficina> {

    Optional<Oficina> findByNomeOficina(String nomeOficina);

    List<Oficina> findByNomeOficinaContainingIgnoreCase(String nomeOficina);

    List<Oficina> findByEspecialidade(String especialidade);

    List<Oficina> findByEspecialidadeContainingIgnoreCase(String especialidade);

    @Query("SELECT o FROM Oficina o JOIN o.endereco e WHERE " +
           "LOWER(e.logradouro) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(e.bairro) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(e.cidade) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(e.estado) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(e.cep) LIKE LOWER(CONCAT('%', :termo, '%'))")
    List<Oficina> procurarPorEndereco(@Param("termo") String termo);
}
