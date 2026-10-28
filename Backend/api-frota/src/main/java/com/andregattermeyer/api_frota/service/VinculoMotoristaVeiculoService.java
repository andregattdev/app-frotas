package com.andregattermeyer.api_frota.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.andregattermeyer.api_frota.dto.response.VinculoMotoristaVeiculoResponseDTO;
import com.andregattermeyer.api_frota.entity.Motorista;
import com.andregattermeyer.api_frota.entity.Veiculo;
import com.andregattermeyer.api_frota.entity.VinculoMotoristaVeiculo;
import com.andregattermeyer.api_frota.repository.VinculoMotoristaVeiculoRepository;

@Service
public class VinculoMotoristaVeiculoService {

    private final VinculoMotoristaVeiculoRepository repository;

    public VinculoMotoristaVeiculoService(VinculoMotoristaVeiculoRepository repository) {
        this.repository = repository;
    }

    public void registrarVinculo(Veiculo veiculo, Motorista motorista) {
        if (repository.findByVeiculoIdAndMotoristaIdAndAtivoTrue(veiculo.getId(), motorista.getId()).isPresent()) return;
        VinculoMotoristaVeiculo vinculo = new VinculoMotoristaVeiculo();
        vinculo.setVeiculo(veiculo);
        vinculo.setMotorista(motorista);
        vinculo.setDataHoraInicio(LocalDateTime.now());
        vinculo.setAtivo(true);
        repository.save(vinculo);
    }

    public void encerrarVinculo(Long veiculoId, Long motoristaId) {
        repository.findByVeiculoIdAndMotoristaIdAndAtivoTrue(veiculoId, motoristaId).ifPresent(vinculo -> {
            vinculo.setAtivo(false);
            vinculo.setDataHoraFim(LocalDateTime.now());
            repository.save(vinculo);
        });
    }

    public void encerrarVinculosDoVeiculo(Long veiculoId) {
        repository.findByVeiculoIdAndAtivoTrue(veiculoId).forEach(vinculo -> {
            vinculo.setAtivo(false);
            vinculo.setDataHoraFim(LocalDateTime.now());
            repository.save(vinculo);
        });
    }

    public List<VinculoMotoristaVeiculoResponseDTO> listarPorVeiculo(Long veiculoId) {
        return repository.findByVeiculoIdOrderByDataHoraInicioDesc(veiculoId).stream().map(this::toDTO).toList();
    }

    private VinculoMotoristaVeiculoResponseDTO toDTO(VinculoMotoristaVeiculo vinculo) {
        VinculoMotoristaVeiculoResponseDTO dto = new VinculoMotoristaVeiculoResponseDTO();
        dto.setId(vinculo.getId());
        dto.setMotoristaId(vinculo.getMotorista().getId());
        dto.setMotoristaNome(vinculo.getMotorista().getNome());
        dto.setMotoristaCnh(vinculo.getMotorista().getCnh());
        dto.setDataHoraInicio(vinculo.getDataHoraInicio());
        dto.setDataHoraFim(vinculo.getDataHoraFim());
        dto.setAtivo(vinculo.isAtivo());
        return dto;
    }
}