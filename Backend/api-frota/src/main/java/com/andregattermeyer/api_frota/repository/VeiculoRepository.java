package com.andregattermeyer.api_frota.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.andregattermeyer.api_frota.entity.Tipo;
import com.andregattermeyer.api_frota.entity.Veiculo;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long>, JpaSpecificationExecutor<Veiculo> {

    // Consultas simples
    List<Veiculo> findByMarca(String marca);

    List<Veiculo> findByModelo(String modelo);

    Optional<Veiculo> findByPlaca(String placa);

    List<Veiculo> findByTipo(Tipo tipo);

    // Busca veículos de um motorista específico (pelo id)
    @Query("SELECT v FROM Veiculo v JOIN v.motoristas m WHERE m.id = :motoristaId")
    List<Veiculo> findByMotoristaId(@Param("motoristaId") Long motoristaId);

    // Busca veículos de um motorista pelo nome + marca
    @Query("SELECT v FROM Veiculo v JOIN v.motoristas m WHERE m.nome = :nomeMotorista AND v.marca = :marca")
    List<Veiculo> findByMotoristaAndMarca(@Param("nomeMotorista") String nomeMotorista, @Param("marca") String marca);

    // Busca veículos de um motorista + tipo
    @Query("SELECT v FROM Veiculo v JOIN v.motoristas m WHERE m.id = :motoristaId AND v.tipo = :tipo")
    List<Veiculo> findByMotoristaAndTipo(@Param("motoristaId") Long motoristaId, @Param("tipo") Tipo tipo);
}