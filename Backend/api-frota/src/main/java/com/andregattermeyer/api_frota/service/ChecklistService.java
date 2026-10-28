package com.andregattermeyer.api_frota.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.andregattermeyer.api_frota.auth.Usuario;
import com.andregattermeyer.api_frota.dto.request.ChecklistRequestDTO;
import com.andregattermeyer.api_frota.dto.response.ChecklistResponseDTO;
import com.andregattermeyer.api_frota.entity.Checklist;
import com.andregattermeyer.api_frota.entity.Motorista;
import com.andregattermeyer.api_frota.entity.Veiculo;
import com.andregattermeyer.api_frota.exception.ResourceNotFoundException;
import com.andregattermeyer.api_frota.mapper.Mapper;
import com.andregattermeyer.api_frota.repository.ChecklistRepository;
import com.andregattermeyer.api_frota.repository.MotoristaRepository;
import com.andregattermeyer.api_frota.repository.UsuarioRepository;
import com.andregattermeyer.api_frota.repository.VeiculoRepository;
import com.andregattermeyer.api_frota.repository.VinculoMotoristaVeiculoRepository;

@Service
public class ChecklistService {

    private final ChecklistRepository checklistRepository;
    private final VeiculoRepository veiculoRepository;
    private final MotoristaRepository motoristaRepository;
    private final UsuarioRepository usuarioRepository;
    private final VinculoMotoristaVeiculoRepository vinculoRepository;

    public ChecklistService(
            ChecklistRepository checklistRepository,
            VeiculoRepository veiculoRepository,
            MotoristaRepository motoristaRepository,
            UsuarioRepository usuarioRepository,
            VinculoMotoristaVeiculoRepository vinculoRepository) {
        this.checklistRepository = checklistRepository;
        this.veiculoRepository = veiculoRepository;
        this.motoristaRepository = motoristaRepository;
        this.usuarioRepository = usuarioRepository;
        this.vinculoRepository = vinculoRepository;
    }

    public List<ChecklistResponseDTO> listarTodos(String busca) {
        return filtrar(checklistRepository.findAll(), busca);
    }

    public ChecklistResponseDTO buscarPorId(Long id) {
        Checklist checklist = checklistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist não encontrado com o id: " + id));
        return Mapper.toChecklistResponseDTO(checklist);
    }

    public ChecklistResponseDTO salvar(ChecklistRequestDTO dto) {
        return salvar(dto, null);
    }

    public ChecklistResponseDTO salvarComoMotorista(ChecklistRequestDTO dto, String username) {
        Motorista motorista = buscarMotoristaAutenticado(username);
        validarVinculoAtivo(dto.getVeiculoId(), motorista.getId());
        return salvar(dto, motorista);
    }

    private ChecklistResponseDTO salvar(ChecklistRequestDTO dto, Motorista motoristaAutenticado) {
        Veiculo veiculo = veiculoRepository.findById(dto.getVeiculoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Veículo não encontrado com o id: " + dto.getVeiculoId()));

        Checklist checklist = Mapper.toChecklistEntity(dto);
        checklist.setVeiculo(veiculo);

        if (motoristaAutenticado != null) {
            checklist.setMotorista(motoristaAutenticado);
        } else if (dto.getMotoristaId() != null) {
            Motorista motorista = motoristaRepository.findById(dto.getMotoristaId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Motorista não encontrado com o id: " + dto.getMotoristaId()));
            checklist.setMotorista(motorista);
        }

        if (checklist.getDataHora() == null) {
            checklist.setDataHora(LocalDateTime.now());
        }

        Checklist salvo = checklistRepository.save(checklist);
        return Mapper.toChecklistResponseDTO(salvo);
    }

    public ChecklistResponseDTO atualizar(Long id, ChecklistRequestDTO dto) {
        Checklist checklistExistente = checklistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist não encontrado com o id: " + id));

        Veiculo veiculo = veiculoRepository.findById(dto.getVeiculoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Veículo não encontrado com o id: " + dto.getVeiculoId()));

        checklistExistente.setDataHora(dto.getDataHora() != null ? dto.getDataHora() : LocalDateTime.now());
        checklistExistente.setQuilometragem(dto.getQuilometragem());
        checklistExistente.setPneus(dto.getPneus());
        checklistExistente.setFarois(dto.getFarois());
        checklistExistente.setLanternas(dto.getLanternas());
        checklistExistente.setFreios(dto.getFreios());
        checklistExistente.setNivelOleo(dto.getNivelOleo());
        checklistExistente.setNivelAgua(dto.getNivelAgua());
        checklistExistente.setDocumentos(dto.getDocumentos());
        checklistExistente.setExtintor(dto.getExtintor());
        checklistExistente.setTriangulo(dto.getTriangulo());
        checklistExistente.setMacaco(dto.getMacaco());
        checklistExistente.setEstepe(dto.getEstepe());
        checklistExistente.setLimpeza(dto.getLimpeza());
        checklistExistente.setObservacoes(dto.getObservacoes());
        checklistExistente.setVeiculo(veiculo);

        if (dto.getMotoristaId() != null) {
            Motorista motorista = motoristaRepository.findById(dto.getMotoristaId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Motorista não encontrado com o id: " + dto.getMotoristaId()));
            checklistExistente.setMotorista(motorista);
        } else {
            checklistExistente.setMotorista(null);
        }

        Checklist atualizado = checklistRepository.save(checklistExistente);
        return Mapper.toChecklistResponseDTO(atualizado);
    }

    public void deletar(Long id) {
        if (!checklistRepository.existsById(id)) {
            throw new ResourceNotFoundException("Checklist não encontrado com o id: " + id);
        }
        checklistRepository.deleteById(id);
    }

    public List<ChecklistResponseDTO> listarPorVeiculo(Long veiculoId, String busca) {
        if (!veiculoRepository.existsById(veiculoId)) {
            throw new ResourceNotFoundException("Veículo não encontrado com o id: " + veiculoId);
        }
        return filtrar(checklistRepository.findByVeiculoId(veiculoId), busca);
    }

    public List<ChecklistResponseDTO> listarPorVeiculoDoMotorista(Long veiculoId, String busca, String username) {
        Motorista motorista = buscarMotoristaAutenticado(username);
        validarVinculoAtivo(veiculoId, motorista.getId());
        return listarPorVeiculo(veiculoId, busca);
    }

    private Motorista buscarMotoristaAutenticado(String username) {
        Usuario usuario = usuarioRepository.findWithMotoristaByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Usuário não está vinculado a um motorista."));
        if (usuario.getMotorista() == null) {
            throw new AccessDeniedException("Usuário não está vinculado a um motorista.");
        }
        return usuario.getMotorista();
    }

    private void validarVinculoAtivo(Long veiculoId, Long motoristaId) {
        if (veiculoId == null
                || vinculoRepository.findByVeiculoIdAndMotoristaIdAndAtivoTrue(veiculoId, motoristaId).isEmpty()) {
            throw new AccessDeniedException("O veículo não está atribuído ao motorista.");
        }
    }

    private List<ChecklistResponseDTO> filtrar(List<Checklist> checklists, String busca) {
        if (busca == null || busca.isBlank())
            return Mapper.toChecklistResponseDTOList(checklists);
        String termo = busca.toLowerCase();
        return Mapper.toChecklistResponseDTOList(checklists.stream()
                .filter(checklist -> contem(checklist.getObservacoes(), termo)
                        || contem(String.valueOf(checklist.getQuilometragem()), termo))
                .toList());
    }

    private boolean contem(String valor, String termo) {
        return valor != null && valor.toLowerCase().contains(termo);
    }

    public List<ChecklistResponseDTO> listarPorMotorista(Long motoristaId) {
        if (!motoristaRepository.existsById(motoristaId)) {
            throw new ResourceNotFoundException("Motorista não encontrado com o id: " + motoristaId);
        }
        return Mapper.toChecklistResponseDTOList(checklistRepository.findByMotoristaId(motoristaId));
    }

    public ChecklistResponseDTO buscarUltimoPorVeiculo(Long veiculoId) {
        if (!veiculoRepository.existsById(veiculoId)) {
            throw new ResourceNotFoundException("Veículo não encontrado com o id: " + veiculoId);
        }

        Checklist checklist = checklistRepository.findUltimoByVeiculoId(veiculoId);
        if (checklist == null) {
            throw new ResourceNotFoundException("Nenhum checklist encontrado para o veículo id: " + veiculoId);
        }
        return Mapper.toChecklistResponseDTO(checklist);
    }
}
