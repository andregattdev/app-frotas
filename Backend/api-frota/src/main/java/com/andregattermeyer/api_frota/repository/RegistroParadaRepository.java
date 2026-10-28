package com.andregattermeyer.api_frota.repository;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.andregattermeyer.api_frota.entity.RegistroParada;
import com.andregattermeyer.api_frota.entity.TipoRegistroParada;

public interface RegistroParadaRepository extends JpaRepository<RegistroParada, Long> {
    Optional<RegistroParada> findByClientRequestId(UUID clientRequestId);
    List<RegistroParada> findByPontoParada_Veiculo_IdOrderByRegistradoEmDesc(Long veiculoId);
    List<RegistroParada> findByPontoParada_IdInOrderByRegistradoEmDesc(List<Long> paradaIds);
    List<RegistroParada> findByPontoParada_IdInAndRegistradoEmGreaterThanEqualAndRegistradoEmLessThanOrderByRegistradoEmDesc(
            List<Long> paradaIds, LocalDateTime inicio, LocalDateTime fim);
    List<RegistroParada> findAllByOrderByRegistradoEmDesc();
    boolean existsByPontoParada_IdAndTipo(Long pontoParadaId, TipoRegistroParada tipo);
    boolean existsByPontoParada_IdAndTipoAndRegistradoEmGreaterThanEqualAndRegistradoEmLessThan(
            Long pontoParadaId, TipoRegistroParada tipo, LocalDateTime inicio, LocalDateTime fim);
    boolean existsByPontoParada_Id(Long pontoParadaId);
}