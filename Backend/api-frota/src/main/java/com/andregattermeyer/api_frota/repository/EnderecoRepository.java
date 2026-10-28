package com.andregattermeyer.api_frota.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.andregattermeyer.api_frota.entity.Endereco;

public interface EnderecoRepository extends JpaRepository<Endereco, Long>, JpaSpecificationExecutor<Endereco> {

    Optional<Endereco> findByCep(String cep);

    List<Endereco> findByCidade(String cidade);

    List<Endereco> findByEstado(String estado);

    List<Endereco> findByBairro(String bairro);
}