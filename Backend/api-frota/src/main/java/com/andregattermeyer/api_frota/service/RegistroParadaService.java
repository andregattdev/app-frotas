package com.andregattermeyer.api_frota.service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.andregattermeyer.api_frota.dto.request.RegistroParadaRequestDTO;
import com.andregattermeyer.api_frota.dto.response.RegistroParadaResponseDTO;
import com.andregattermeyer.api_frota.entity.RegistroParada;
import com.andregattermeyer.api_frota.entity.StatusRotaDiaria;
import com.andregattermeyer.api_frota.entity.TipoRegistroParada;
import com.andregattermeyer.api_frota.exception.BadRequestException;
import com.andregattermeyer.api_frota.exception.ResourceNotFoundException;
import com.andregattermeyer.api_frota.repository.PontoParadaRepository;
import com.andregattermeyer.api_frota.repository.RegistroParadaRepository;
import com.andregattermeyer.api_frota.repository.UsuarioRepository;
import com.andregattermeyer.api_frota.repository.VinculoMotoristaVeiculoRepository;

@Service
public class RegistroParadaService {

    private final PontoParadaRepository pontoParadaRepository;
    private final RegistroParadaRepository registroParadaRepository;
    private final UsuarioRepository usuarioRepository;
    private final VinculoMotoristaVeiculoRepository vinculoRepository;
    private final FotoParadaStorageService fotoStorage;

    public RegistroParadaService(
            PontoParadaRepository pontoParadaRepository,
            RegistroParadaRepository registroParadaRepository,
            UsuarioRepository usuarioRepository,
            VinculoMotoristaVeiculoRepository vinculoRepository,
            FotoParadaStorageService fotoStorage) {
        this.pontoParadaRepository = pontoParadaRepository;
        this.registroParadaRepository = registroParadaRepository;
        this.usuarioRepository = usuarioRepository;
        this.vinculoRepository = vinculoRepository;
        this.fotoStorage = fotoStorage;
    }

    @Transactional
    public RegistroParadaResponseDTO registrar(
            Long paradaId,
            String username,
            RegistroParadaRequestDTO request,
            MultipartFile foto,
            Double latitude,
            Double longitude) {
        var usuario = usuarioRepository.findWithMotoristaByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("A conta não está vinculada a um motorista."));
        var motorista = usuario.getMotorista();
        if (motorista == null) {
            throw new AccessDeniedException("A conta não está vinculada a um motorista.");
        }

        var registroExistente = registroParadaRepository.findByClientRequestId(request.clientRequestId());
        if (registroExistente.isPresent()) {
            var existente = registroExistente.get();
            if (!existente.getUsuarioMotorista().getId().equals(usuario.getId())) {
                throw new AccessDeniedException("Este identificador de registro já foi utilizado.");
            }
            if (!existente.getPontoParada().getId().equals(paradaId)
                    || existente.getTipo() != request.tipo()
                    || !Objects.equals(existente.getObservacao(), normalizarObservacao(request.observacao()))) {
                throw new BadRequestException("O identificador já está associado a outro registro.");
            }
            return RegistroParadaResponseDTO.from(existente);
        }

        if ((latitude == null) != (longitude == null)
                || (latitude != null && (!Double.isFinite(latitude) || latitude < -90 || latitude > 90))
                || (longitude != null && (!Double.isFinite(longitude) || longitude < -180 || longitude > 180))) {
            throw new BadRequestException("A localização GPS recebida é inválida.");
        }

        var parada = pontoParadaRepository.findById(paradaId)
                .orElseThrow(() -> new ResourceNotFoundException("Parada não encontrada."));
        if (parada.getRotaDiaria() != null) {
            var rota = parada.getRotaDiaria();
            if (!rota.getMotorista().getId().equals(motorista.getId())) {
                throw new AccessDeniedException("Esta parada não pertence a uma rota atribuída ao motorista.");
            }
            if (!rota.getDataRota().equals(java.time.LocalDate.now())) {
                throw new BadRequestException("Esta rota só pode ser executada na data programada.");
            }
            if (rota.getStatus() == StatusRotaDiaria.FINALIZADA) {
                throw new BadRequestException("Esta rota já foi finalizada.");
            }
        } else if (vinculoRepository.findByVeiculoIdAndMotoristaIdAndAtivoTrue(
                    parada.getVeiculo().getId(), motorista.getId()).isEmpty()) {
            throw new AccessDeniedException("Esta parada não pertence a uma rota atribuída ao motorista.");
        }
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicioDoDia = hoje.atStartOfDay();
        LocalDateTime inicioDoProximoDia = hoje.plusDays(1).atStartOfDay();
        if (registroParadaRepository
                .existsByPontoParada_IdAndTipoAndRegistradoEmGreaterThanEqualAndRegistradoEmLessThan(
                        paradaId, TipoRegistroParada.CONCLUIDA, inicioDoDia, inicioDoProximoDia)) {
            throw new BadRequestException("Esta parada já foi concluída.");
        }

        String observacao = normalizarObservacao(request.observacao());
        if (request.tipo() == TipoRegistroParada.OCORRENCIA && (observacao == null || observacao.isBlank())) {
            throw new BadRequestException("Descreva o motivo da ocorrência.");
        }

        String tipoFoto = fotoStorage.validar(foto);
        var registro = new RegistroParada();
        registro.setPontoParada(parada);
        registro.setUsuarioMotorista(usuario);
        registro.setTipo(request.tipo());
        registro.setObservacao(observacao == null || observacao.isBlank() ? null : observacao);
        registro.setClientRequestId(request.clientRequestId());
        LocalDateTime registradoEm = LocalDateTime.now();
        registro.setRegistradoEm(registradoEm);
        if (latitude != null) {
            registro.setLatitudeRegistro(latitude);
            registro.setLongitudeRegistro(longitude);
        }

        String nomeFoto = fotoStorage.armazenar(foto, tipoFoto, registradoEm, latitude, longitude);
        registro.setFotoNomeArquivo(nomeFoto);
        if (parada.getRotaDiaria() != null
                && parada.getRotaDiaria().getStatus() == StatusRotaDiaria.PLANEJADA) {
            parada.getRotaDiaria().setStatus(StatusRotaDiaria.EM_ANDAMENTO);
        }
        try {
            return RegistroParadaResponseDTO.from(registroParadaRepository.save(registro));
        } catch (RuntimeException erro) {
            fotoStorage.excluir(nomeFoto);
            throw erro;
        }
    }

    private String normalizarObservacao(String observacao) {
        return observacao == null || observacao.trim().isBlank() ? null : observacao.trim();
    }

    @Transactional(readOnly = true)
    public List<RegistroParadaResponseDTO> listarPorVeiculo(Long veiculoId) {
        return registroParadaRepository.findByPontoParada_Veiculo_IdOrderByRegistradoEmDesc(veiculoId).stream()
                .map(RegistroParadaResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RegistroParadaResponseDTO> listarPorParadas(List<Long> paradaIds) {
        if (paradaIds.isEmpty()) {
            return List.of();
        }
        LocalDate hoje = LocalDate.now();
        return registroParadaRepository
                .findByPontoParada_IdInAndRegistradoEmGreaterThanEqualAndRegistradoEmLessThanOrderByRegistradoEmDesc(
                        paradaIds, hoje.atStartOfDay(), hoje.plusDays(1).atStartOfDay()).stream()
                .map(RegistroParadaResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RegistroParadaResponseDTO> listarTodos() {
        return registroParadaRepository.findAllByOrderByRegistradoEmDesc().stream()
                .map(RegistroParadaResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Resource obterFoto(Long registroId) {
        var registro = registroParadaRepository.findById(registroId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de parada não encontrado."));
        if (registro.getFotoNomeArquivo() == null) {
            throw new ResourceNotFoundException("Este registro não possui foto.");
        }
        return fotoStorage.carregar(registro.getFotoNomeArquivo());
    }
}