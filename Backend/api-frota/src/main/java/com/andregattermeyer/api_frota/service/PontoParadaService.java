package com.andregattermeyer.api_frota.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.andregattermeyer.api_frota.dto.request.PontoParadaRequestDTO;
import com.andregattermeyer.api_frota.dto.response.PontoParadaResponseDTO;
import com.andregattermeyer.api_frota.entity.PontoParada;
import com.andregattermeyer.api_frota.entity.Veiculo;
import com.andregattermeyer.api_frota.exception.BadRequestException;
import com.andregattermeyer.api_frota.exception.ResourceNotFoundException;
import com.andregattermeyer.api_frota.repository.PontoParadaRepository;
import com.andregattermeyer.api_frota.repository.VeiculoRepository;

@Service
public class PontoParadaService {

    private final PontoParadaRepository pontoParadaRepository;
    private final VeiculoRepository veiculoRepository;

    public PontoParadaService(
            PontoParadaRepository pontoParadaRepository,
            VeiculoRepository veiculoRepository) {
        this.pontoParadaRepository = pontoParadaRepository;
        this.veiculoRepository = veiculoRepository;
    }

    public List<PontoParadaResponseDTO> listarPorVeiculo(Long veiculoId) {
        validarVeiculo(veiculoId);
        return pontoParadaRepository.findByVeiculoIdAndRotaDiariaIsNullOrderByOrdemAscIdAsc(veiculoId).stream()
                .map(PontoParadaResponseDTO::from)
                .toList();
    }

    public PontoParadaResponseDTO criar(Long veiculoId, PontoParadaRequestDTO dto) {
        Veiculo veiculo = buscarVeiculo(veiculoId);
        List<PontoParada> pontos = pontoParadaRepository.findByVeiculoIdAndRotaDiariaIsNullOrderByOrdemAscIdAsc(veiculoId);
        PontoParada ponto = new PontoParada();
        atualizarDados(ponto, dto);
        ponto.setOrdem(pontos.size() + 1);
        ponto.setVeiculo(veiculo);
        return PontoParadaResponseDTO.from(pontoParadaRepository.save(ponto));
    }

    public PontoParadaResponseDTO atualizar(Long veiculoId, Long pontoId, PontoParadaRequestDTO dto) {
        PontoParada ponto = buscarPonto(veiculoId, pontoId);
        atualizarDados(ponto, dto);
        return PontoParadaResponseDTO.from(pontoParadaRepository.save(ponto));
    }

    @Transactional
    public void excluir(Long veiculoId, Long pontoId) {
        pontoParadaRepository.delete(buscarPonto(veiculoId, pontoId));
        reorganizarOrdem(veiculoId, pontoParadaRepository.findByVeiculoIdAndRotaDiariaIsNullOrderByOrdemAscIdAsc(veiculoId));
    }

    @Transactional
    public List<PontoParadaResponseDTO> reordenar(Long veiculoId, List<Long> ids) {
        validarVeiculo(veiculoId);
        List<PontoParada> pontos = pontoParadaRepository.findByVeiculoIdAndRotaDiariaIsNullOrderByOrdemAscIdAsc(veiculoId);
        Set<Long> idsRecebidos = new HashSet<>(ids);
        Set<Long> idsExistentes = pontos.stream()
                .map(ponto -> ponto.getId())
                .collect(Collectors.toSet());
        if (ids.size() != pontos.size()
                || idsRecebidos.size() != ids.size()
                || !idsRecebidos.equals(idsExistentes)) {
            throw new BadRequestException("A ordem informada deve conter cada parada da rota exatamente uma vez.");
        }

        var pontosPorId = pontos.stream()
                .collect(Collectors.toMap(ponto -> ponto.getId(), ponto -> ponto));
        for (int indice = 0; indice < ids.size(); indice++) {
            pontosPorId.get(ids.get(indice)).setOrdem(indice + 1);
        }

        List<PontoParada> reordenados = pontoParadaRepository.saveAll(pontosPorId.values());
        reordenados.sort((primeiro, segundo) -> primeiro.getOrdem().compareTo(segundo.getOrdem()));
        return reordenados.stream()
                .map(PontoParadaResponseDTO::from)
                .toList();
    }

    private Veiculo buscarVeiculo(Long veiculoId) {
        return veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado com o id: " + veiculoId));
    }

    private void validarVeiculo(Long veiculoId) {
        if (!veiculoRepository.existsById(veiculoId)) {
            throw new ResourceNotFoundException("Veículo não encontrado com o id: " + veiculoId);
        }
    }

    private PontoParada buscarPonto(Long veiculoId, Long pontoId) {
        return pontoParadaRepository.findByIdAndVeiculoIdAndRotaDiariaIsNull(pontoId, veiculoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Parada não encontrada para o veículo informado."));
    }

    private void atualizarDados(PontoParada ponto, PontoParadaRequestDTO dto) {
        ponto.setDescricao(dto.getDescricao().trim());
        ponto.setNomeEstabelecimento(normalizarOpcional(dto.getNomeEstabelecimento()));
        ponto.setEndereco(normalizarOpcional(dto.getEndereco()));
        ponto.setTipo(dto.getTipo());
        ponto.setLatitude(dto.getLatitude());
        ponto.setLongitude(dto.getLongitude());
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private void reorganizarOrdem(Long veiculoId, List<PontoParada> pontos) {
        for (int indice = 0; indice < pontos.size(); indice++) {
            pontos.get(indice).setOrdem(indice + 1);
        }
        pontoParadaRepository.saveAll(pontos);
    }
}
