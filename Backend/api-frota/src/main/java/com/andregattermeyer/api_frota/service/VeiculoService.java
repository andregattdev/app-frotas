package com.andregattermeyer.api_frota.service;

import java.util.List;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.andregattermeyer.api_frota.dto.request.VeiculoRequestDTO;
import com.andregattermeyer.api_frota.dto.response.MotoristaResponseDTO;
import com.andregattermeyer.api_frota.dto.response.VeiculoResponseDTO;
import com.andregattermeyer.api_frota.entity.Motorista;
import com.andregattermeyer.api_frota.entity.Tipo;
import com.andregattermeyer.api_frota.entity.Veiculo;
import com.andregattermeyer.api_frota.exception.BadRequestException;
import com.andregattermeyer.api_frota.exception.ResourceNotFoundException;
import com.andregattermeyer.api_frota.mapper.Mapper;
import com.andregattermeyer.api_frota.repository.MotoristaRepository;
import com.andregattermeyer.api_frota.repository.UsuarioRepository;
import com.andregattermeyer.api_frota.repository.VeiculoRepository;
import com.andregattermeyer.api_frota.repository.SearchSpecifications;

@Service
public class VeiculoService {

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private MotoristaRepository motoristaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private VinculoMotoristaVeiculoService vinculoService;

    public List<VeiculoResponseDTO> listarTodos(String busca) {
        if (busca == null || busca.isBlank()) {
            return Mapper.toVeiculoResponseDTOList(veiculoRepository.findAll());
        }

        String termo = busca.toLowerCase();
        return Mapper.toVeiculoResponseDTOList(veiculoRepository.findAll().stream()
                .filter(veiculo -> contem(veiculo.getPlaca(), termo) || contem(veiculo.getModelo(), termo)
                        || contem(veiculo.getMarca(), termo) || contem(veiculo.getFrota(), termo)
                        || contem(String.valueOf(veiculo.getTipo()), termo))
                .toList());
    }

    public Page<VeiculoResponseDTO> listarPagina(String busca, Pageable pageable) {
        return veiculoRepository.findAll(
                SearchSpecifications.containsAnyIgnoreCase(busca, "placa", "modelo", "marca", "frota", "tipo"),
                pageable).map(Mapper::toVeiculoResponseDTO);
    }

    private boolean contem(String valor, String termo) {
        return valor != null && valor.toLowerCase().contains(termo);
    }

    public VeiculoResponseDTO buscarPorId(Long id) {
        Veiculo veiculo = veiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Veiculo nao encontrado com o id: " + id));
        return Mapper.toVeiculoResponseDTO(veiculo);
    }

    public VeiculoResponseDTO buscarPorPlaca(String placa) {
        Veiculo veiculo = veiculoRepository.findByPlaca(placa)
                .orElseThrow(() -> new ResourceNotFoundException("Veiculo nao encontrado com a placa: " + placa));
        return Mapper.toVeiculoResponseDTO(veiculo);
    }

    public List<VeiculoResponseDTO> buscarPorMarca(String marca) {
        return Mapper.toVeiculoResponseDTOList(veiculoRepository.findByMarca(marca));
    }

    public List<VeiculoResponseDTO> buscarPorModelo(String modelo) {
        return Mapper.toVeiculoResponseDTOList(veiculoRepository.findByModelo(modelo));
    }

    public List<VeiculoResponseDTO> buscarPorTipo(Tipo tipo) {
        return Mapper.toVeiculoResponseDTOList(veiculoRepository.findByTipo(tipo));
    }

    public VeiculoResponseDTO salvar(VeiculoRequestDTO dto) {
        veiculoRepository.findByPlaca(dto.getPlaca())
                .ifPresent(v -> {
                    throw new BadRequestException("Ja existe um veiculo cadastrado com a placa: " + dto.getPlaca());
                });

        Veiculo veiculo = Mapper.toVeiculoEntity(dto);
        Veiculo salvo = veiculoRepository.save(veiculo);
        return Mapper.toVeiculoResponseDTO(salvo);
    }

    public VeiculoResponseDTO atualizar(Long id, VeiculoRequestDTO dto) {
        Veiculo veiculoExistente = veiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Veiculo nao encontrado com o id: " + id));

        veiculoRepository.findByPlaca(dto.getPlaca())
                .filter(v -> !v.getId().equals(id))
                .ifPresent(v -> {
                    throw new BadRequestException("Ja existe um veiculo cadastrado com a placa: " + dto.getPlaca());
                });

        veiculoExistente.setPlaca(dto.getPlaca());
        veiculoExistente.setModelo(dto.getModelo());
        veiculoExistente.setMarca(dto.getMarca());
        veiculoExistente.setCor(dto.getCor());
        veiculoExistente.setFrota(dto.getFrota());
        veiculoExistente.setRenavan(dto.getRenavan());
        veiculoExistente.setTipo(dto.getTipo());

        Veiculo atualizado = veiculoRepository.save(veiculoExistente);
        return Mapper.toVeiculoResponseDTO(atualizado);
    }

    public void deletar(Long id) {
        if (!veiculoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Veiculo nao encontrado com o id: " + id);
        }
        veiculoRepository.deleteById(id);
    }

    // ==================== VÍNCULO MOTORISTA ====================

    @Transactional
    public VeiculoResponseDTO vincularMotorista(Long veiculoId, Long motoristaId) {
        Veiculo veiculo = veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> new ResourceNotFoundException("Veiculo nao encontrado com o id: " + veiculoId));

        Motorista motorista = motoristaRepository.findById(motoristaId)
                .orElseThrow(() -> new ResourceNotFoundException("Motorista nao encontrado com o id: " + motoristaId));

        if (usuarioRepository.findByMotoristaId(motoristaId)
                .filter(usuario -> usuario.getRoles().contains(com.andregattermeyer.api_frota.auth.Role.MOTORISTA))
                .isEmpty()) {
            throw new BadRequestException("O motorista precisa ter uma conta com perfil de motorista antes da atribuição.");
        }

        if (veiculo.getMotoristas().stream().anyMatch(m -> m.getId().equals(motoristaId))) {
            throw new BadRequestException("Este motorista já está vinculado a este veículo");
        }

        if (!veiculo.getMotoristas().isEmpty()) {
            veiculo.getMotoristas().clear();
            veiculoRepository.saveAndFlush(veiculo);
            vinculoService.encerrarVinculosDoVeiculo(veiculoId);
        }

        veiculo.getMotoristas().add(motorista);
        Veiculo atualizado = veiculoRepository.saveAndFlush(veiculo);
        vinculoService.registrarVinculo(atualizado, motorista);
        return Mapper.toVeiculoResponseDTO(atualizado);
    }

    public VeiculoResponseDTO desvincularMotorista(Long veiculoId, Long motoristaId) {
        Veiculo veiculo = veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> new ResourceNotFoundException("Veiculo nao encontrado com o id: " + veiculoId));

        boolean removido = veiculo.getMotoristas().removeIf(m -> m.getId().equals(motoristaId));

        if (!removido) {
            throw new ResourceNotFoundException("Motorista não está vinculado a este veículo");
        }

        Veiculo atualizado = veiculoRepository.save(veiculo);
        vinculoService.encerrarVinculo(veiculoId, motoristaId);
        return Mapper.toVeiculoResponseDTO(atualizado);
    }

    public List<MotoristaResponseDTO> listarMotoristasDoVeiculo(Long veiculoId) {
        Veiculo veiculo = veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> new ResourceNotFoundException("Veiculo nao encontrado com o id: " + veiculoId));

        return Mapper.toMotoristaResponseDTOList(veiculo.getMotoristas());
    }
}
