package com.andregattermeyer.api_frota.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.andregattermeyer.api_frota.auth.Usuario;
import com.andregattermeyer.api_frota.dto.request.CriarEncaminhamentoManutencaoRequest;
import com.andregattermeyer.api_frota.dto.response.EncaminhamentoManutencaoResponse;
import com.andregattermeyer.api_frota.dto.response.EnderecoResponseDTO;
import com.andregattermeyer.api_frota.dto.response.OficinaResponseDTO;
import com.andregattermeyer.api_frota.entity.EncaminhamentoManutencao;
import com.andregattermeyer.api_frota.exception.ResourceNotFoundException;
import com.andregattermeyer.api_frota.mapper.Mapper;
import com.andregattermeyer.api_frota.repository.EncaminhamentoManutencaoRepository;
import com.andregattermeyer.api_frota.repository.OficinaRepository;
import com.andregattermeyer.api_frota.repository.UsuarioRepository;
import com.andregattermeyer.api_frota.repository.VeiculoRepository;
import com.andregattermeyer.api_frota.repository.VinculoMotoristaVeiculoRepository;

@Service
public class EncaminhamentoManutencaoService {

    private final EncaminhamentoManutencaoRepository encaminhamentoRepository;
    private final VeiculoRepository veiculoRepository;
    private final OficinaRepository oficinaRepository;
    private final VinculoMotoristaVeiculoRepository vinculoRepository;
    private final UsuarioRepository usuarioRepository;

    public EncaminhamentoManutencaoService(
            EncaminhamentoManutencaoRepository encaminhamentoRepository,
            VeiculoRepository veiculoRepository,
            OficinaRepository oficinaRepository,
            VinculoMotoristaVeiculoRepository vinculoRepository,
            UsuarioRepository usuarioRepository) {
        this.encaminhamentoRepository = encaminhamentoRepository;
        this.veiculoRepository = veiculoRepository;
        this.oficinaRepository = oficinaRepository;
        this.vinculoRepository = vinculoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<EncaminhamentoManutencaoResponse> listarAtivos() {
        return encaminhamentoRepository.findByAtivoTrueOrderByEnviadoEmDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EncaminhamentoManutencaoResponse> listarDoMotorista(String username) {
        var motorista = buscarMotorista(username);
        return encaminhamentoRepository.findByMotoristaIdAndAtivoTrueOrderByEnviadoEmDesc(motorista.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EncaminhamentoManutencaoResponse criar(CriarEncaminhamentoManutencaoRequest request) {
        var veiculo = veiculoRepository.findById(request.veiculoId())
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado."));
        var oficina = oficinaRepository.findById(request.oficinaId())
                .orElseThrow(() -> new ResourceNotFoundException("Oficina não encontrada."));
        var vinculo = vinculoRepository.findByVeiculoIdAndAtivoTrue(request.veiculoId()).stream()
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException(
                        "O veículo precisa estar atribuído a um motorista para enviar o alerta."));

        encaminhamentoRepository.findByVeiculoIdAndAtivoTrue(veiculo.getId()).ifPresent(anterior -> {
            anterior.setAtivo(false);
            encaminhamentoRepository.save(anterior);
        });

        var encaminhamento = new EncaminhamentoManutencao();
        encaminhamento.setVeiculo(veiculo);
        encaminhamento.setMotorista(vinculo.getMotorista());
        encaminhamento.setOficinaId(oficina.getId());
        encaminhamento.setOficinaNome(oficina.getNomeOficina());
        encaminhamento.setOficinaTelefone(oficina.getTelefone());
        encaminhamento.setOficinaEspecialidade(oficina.getEspecialidade());
        if (oficina.getEndereco() != null) {
            encaminhamento.setOficinaLogradouro(oficina.getEndereco().getLogradouro());
            encaminhamento.setOficinaNumero(oficina.getEndereco().getNumero());
            encaminhamento.setOficinaComplemento(oficina.getEndereco().getComplemento());
            encaminhamento.setOficinaBairro(oficina.getEndereco().getBairro());
            encaminhamento.setOficinaCidade(oficina.getEndereco().getCidade());
            encaminhamento.setOficinaEstado(oficina.getEndereco().getEstado());
            encaminhamento.setOficinaCep(oficina.getEndereco().getCep());
        }
        encaminhamento.setOrientacao(request.orientacao() == null ? null : request.orientacao().trim());
        encaminhamento.setEnviadoEm(LocalDateTime.now());
        return toResponse(encaminhamentoRepository.save(encaminhamento));
    }

    @Transactional
    public void concluir(Long id) {
        var encaminhamento = encaminhamentoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encaminhamento ativo não encontrado."));
        encaminhamento.setAtivo(false);
        encaminhamentoRepository.save(encaminhamento);
    }

    private com.andregattermeyer.api_frota.entity.Motorista buscarMotorista(String username) {
        Usuario usuario = usuarioRepository.findWithMotoristaByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("A conta não está vinculada a um motorista."));
        if (usuario.getMotorista() == null) {
            throw new AccessDeniedException("A conta não está vinculada a um motorista.");
        }
        return usuario.getMotorista();
    }

    private EncaminhamentoManutencaoResponse toResponse(EncaminhamentoManutencao encaminhamento) {
        var endereco = new EnderecoResponseDTO();
        endereco.setLogradouro(encaminhamento.getOficinaLogradouro());
        endereco.setNumero(encaminhamento.getOficinaNumero());
        endereco.setComplemento(encaminhamento.getOficinaComplemento());
        endereco.setBairro(encaminhamento.getOficinaBairro());
        endereco.setCidade(encaminhamento.getOficinaCidade());
        endereco.setEstado(encaminhamento.getOficinaEstado());
        endereco.setCep(encaminhamento.getOficinaCep());
        var oficina = new OficinaResponseDTO();
        oficina.setId(encaminhamento.getOficinaId());
        oficina.setNomeOficina(encaminhamento.getOficinaNome());
        oficina.setTelefone(encaminhamento.getOficinaTelefone());
        oficina.setEspecialidade(encaminhamento.getOficinaEspecialidade());
        oficina.setEndereco(endereco);
        return new EncaminhamentoManutencaoResponse(
                encaminhamento.getId(),
                Mapper.toVeiculoResponseDTO(encaminhamento.getVeiculo()),
                oficina,
                encaminhamento.getOrientacao(),
                encaminhamento.getEnviadoEm());
    }
}
