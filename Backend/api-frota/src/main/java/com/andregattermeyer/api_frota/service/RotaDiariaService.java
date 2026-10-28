package com.andregattermeyer.api_frota.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.andregattermeyer.api_frota.auth.Role;
import com.andregattermeyer.api_frota.dto.request.ParadaRotaDiariaRequestDTO;
import com.andregattermeyer.api_frota.dto.request.ParadaRotaDiariaAtualizacaoRequestDTO;
import com.andregattermeyer.api_frota.dto.request.RotaDiariaRequestDTO;
import com.andregattermeyer.api_frota.dto.request.AtualizarRotaDiariaRequestDTO;
import com.andregattermeyer.api_frota.dto.response.PontoParadaResponseDTO;
import com.andregattermeyer.api_frota.dto.response.RegistroParadaResponseDTO;
import com.andregattermeyer.api_frota.dto.response.RotaDiariaResponseDTO;
import com.andregattermeyer.api_frota.dto.response.RotaMotoristaResponseDTO;
import com.andregattermeyer.api_frota.entity.PontoParada;
import com.andregattermeyer.api_frota.entity.RegistroParada;
import com.andregattermeyer.api_frota.entity.RotaDiaria;
import com.andregattermeyer.api_frota.entity.StatusRotaDiaria;
import com.andregattermeyer.api_frota.exception.BadRequestException;
import com.andregattermeyer.api_frota.exception.ResourceNotFoundException;
import com.andregattermeyer.api_frota.repository.MotoristaRepository;
import com.andregattermeyer.api_frota.repository.PontoParadaRepository;
import com.andregattermeyer.api_frota.repository.RegistroParadaRepository;
import com.andregattermeyer.api_frota.repository.RotaDiariaRepository;
import com.andregattermeyer.api_frota.repository.UsuarioRepository;
import com.andregattermeyer.api_frota.repository.VeiculoRepository;
import com.andregattermeyer.api_frota.repository.VinculoMotoristaVeiculoRepository;

@Service
public class RotaDiariaService {

    private final RotaDiariaRepository rotaRepository;
    private final PontoParadaRepository paradaRepository;
    private final RegistroParadaRepository registroRepository;
    private final MotoristaRepository motoristaRepository;
    private final VeiculoRepository veiculoRepository;
    private final UsuarioRepository usuarioRepository;
    private final VinculoMotoristaVeiculoRepository vinculoRepository;

    public RotaDiariaService(
            RotaDiariaRepository rotaRepository,
            PontoParadaRepository paradaRepository,
            RegistroParadaRepository registroRepository,
            MotoristaRepository motoristaRepository,
            VeiculoRepository veiculoRepository,
            UsuarioRepository usuarioRepository,
            VinculoMotoristaVeiculoRepository vinculoRepository) {
        this.rotaRepository = rotaRepository;
        this.paradaRepository = paradaRepository;
        this.registroRepository = registroRepository;
        this.motoristaRepository = motoristaRepository;
        this.veiculoRepository = veiculoRepository;
        this.usuarioRepository = usuarioRepository;
        this.vinculoRepository = vinculoRepository;
    }

    @Transactional
    public RotaDiariaResponseDTO criar(RotaDiariaRequestDTO request) {
        if (request.dataRota().isBefore(LocalDate.now())) {
            throw new BadRequestException("A data da rota não pode estar no passado.");
        }
        var motorista = motoristaRepository.findById(request.motoristaId())
                .orElseThrow(() -> new ResourceNotFoundException("Motorista não encontrado."));
        var usuario = usuarioRepository.findByMotoristaId(motorista.getId())
                .orElseThrow(() -> new BadRequestException("O motorista precisa ter uma conta vinculada."));
        if (!usuario.getRoles().contains(Role.MOTORISTA)) {
            throw new BadRequestException("A conta vinculada não possui o perfil de motorista.");
        }
        var veiculo = veiculoRepository.findById(request.veiculoId())
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado."));
        if (rotaRepository.existsByMotorista_IdAndDataRota(motorista.getId(), request.dataRota())) {
            throw new BadRequestException("Este motorista já possui uma rota programada para essa data.");
        }
        if (rotaRepository.existsByVeiculo_IdAndDataRota(veiculo.getId(), request.dataRota())) {
            throw new BadRequestException("Este veículo já possui uma rota programada para essa data.");
        }

        var rota = new RotaDiaria();
        rota.setMotorista(motorista);
        rota.setVeiculo(veiculo);
        rota.setDataRota(request.dataRota());
        rota.setCriadaEm(LocalDateTime.now());
        rota = rotaRepository.save(rota);

        List<PontoParada> paradas = new java.util.ArrayList<>();
        for (int indice = 0; indice < request.paradas().size(); indice++) {
            ParadaRotaDiariaRequestDTO paradaRequest = request.paradas().get(indice);
            PontoParada parada = new PontoParada();
            parada.setDescricao(paradaRequest.descricao().trim());
            parada.setNomeEstabelecimento(limpar(paradaRequest.nomeEstabelecimento()));
            parada.setEndereco(limpar(paradaRequest.endereco()));
            parada.setTipo(paradaRequest.tipo());
            parada.setLatitude(paradaRequest.latitude());
            parada.setLongitude(paradaRequest.longitude());
            parada.setOrdem(indice + 1);
            parada.setVeiculo(veiculo);
            parada.setRotaDiaria(rota);
            paradas.add(parada);
        }

        return respostaAdmin(rota, paradaRepository.saveAll(paradas));
    }

    @Transactional(readOnly = true)
    public List<RotaDiariaResponseDTO> listarTodas() {
        return rotaRepository.findAllByOrderByDataRotaDescIdDesc().stream()
                .map(rota -> respostaAdmin(rota,
                        paradaRepository.findByRotaDiaria_IdOrderByOrdemAscIdAsc(rota.getId())))
                .toList();
    }

    @Transactional
    public RotaDiariaResponseDTO atualizar(Long rotaId, AtualizarRotaDiariaRequestDTO request) {
        var rota = rotaRepository.findById(rotaId)
                .orElseThrow(() -> new ResourceNotFoundException("Rota diária não encontrada."));
        if (rota.getStatus() == StatusRotaDiaria.FINALIZADA) {
            throw new BadRequestException("Uma rota finalizada não pode ser alterada.");
        }

        List<PontoParada> existentes = paradaRepository.findByRotaDiaria_IdOrderByOrdemAscIdAsc(rotaId);
        Map<Long, PontoParada> porId = new HashMap<>();
        Map<Long, Integer> ordemOriginal = new HashMap<>();
        for (int indice = 0; indice < existentes.size(); indice++) {
            PontoParada parada = existentes.get(indice);
            porId.put(parada.getId(), parada);
            ordemOriginal.put(parada.getId(), indice);
        }
        Set<Long> idsRecebidos = new HashSet<>();
        List<PontoParada> atualizadas = new java.util.ArrayList<>();

        for (int indice = 0; indice < request.paradas().size(); indice++) {
            ParadaRotaDiariaAtualizacaoRequestDTO dados = request.paradas().get(indice);
            PontoParada parada;
            if (dados.id() == null) {
                parada = new PontoParada();
                parada.setRotaDiaria(rota);
                parada.setVeiculo(rota.getVeiculo());
            } else {
                parada = porId.get(dados.id());
                if (parada == null || !idsRecebidos.add(dados.id())) {
                    throw new BadRequestException("A lista contém uma parada inválida ou duplicada.");
                }
                if (registroRepository.existsByPontoParada_Id(parada.getId())) {
                    if (ordemOriginal.get(parada.getId()) != indice || !mesmaParada(parada, dados)) {
                        throw new BadRequestException(
                                "Paradas com registros não podem ser alteradas nem reordenadas.");
                    }
                    atualizadas.add(parada);
                    continue;
                }
            }
            parada.setDescricao(dados.descricao().trim());
            parada.setNomeEstabelecimento(limpar(dados.nomeEstabelecimento()));
            parada.setEndereco(limpar(dados.endereco()));
            parada.setTipo(dados.tipo());
            parada.setLatitude(dados.latitude());
            parada.setLongitude(dados.longitude());
            parada.setOrdem(indice + 1);
            atualizadas.add(parada);
        }

        if (!idsRecebidos.equals(porId.keySet())) {
            throw new BadRequestException("Não é permitido remover paradas ao atualizar uma rota em execução.");
        }

        List<PontoParada> salvas = paradaRepository.saveAll(atualizadas);
        return respostaAdmin(rota, salvas);
    }

    @Transactional(readOnly = true)
    public List<RotaMotoristaResponseDTO> listarDoMotorista(String username) {
        var usuario = usuarioRepository.findWithMotoristaByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Conta de motorista não encontrada."));
        if (usuario.getMotorista() == null) {
            throw new AccessDeniedException("A conta não está vinculada a um motorista.");
        }
        return rotaRepository.findByMotorista_IdAndDataRotaGreaterThanEqualOrderByDataRotaAscIdAsc(
                        usuario.getMotorista().getId(), LocalDate.now()).stream()
                .map(rota -> {
                    var paradas = paradaRepository.findByRotaDiaria_IdOrderByOrdemAscIdAsc(rota.getId());
                    var registros = registrosDasParadas(paradas);
                    return new RotaMotoristaResponseDTO(
                            rota.getId(),
                            rota.getStatus(),
                            rota.getDataRota(),
                            rota.getFinalizadaEm(),
                            com.andregattermeyer.api_frota.mapper.Mapper.toVeiculoResponseDTO(rota.getVeiculo()),
                            rota.getCriadaEm(),
                            paradas.stream().map(PontoParadaResponseDTO::from).toList(),
                            registros);
                })
                .toList();
    }

    @Transactional
    public RotaMotoristaResponseDTO finalizar(Long rotaId, String username) {
        var usuario = usuarioRepository.findWithMotoristaByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Conta de motorista não encontrada."));
        if (usuario.getMotorista() == null) {
            throw new AccessDeniedException("A conta não está vinculada a um motorista.");
        }
        var rota = rotaRepository.findByIdAndMotorista_Id(rotaId, usuario.getMotorista().getId())
                .orElseThrow(() -> new AccessDeniedException("Esta rota não pertence ao motorista."));
        if (!rota.getDataRota().equals(LocalDate.now())) {
            throw new BadRequestException("A rota só pode ser finalizada na data programada.");
        }
        if (rota.getStatus() == StatusRotaDiaria.FINALIZADA) {
            throw new BadRequestException("Esta rota já foi finalizada.");
        }
        rota.setStatus(StatusRotaDiaria.FINALIZADA);
        rota.setFinalizadaEm(LocalDateTime.now());
        rotaRepository.save(rota);

        var paradas = paradaRepository.findByRotaDiaria_IdOrderByOrdemAscIdAsc(rotaId);
        var registros = registrosDasParadas(paradas);
        return new RotaMotoristaResponseDTO(
                rota.getId(),
                rota.getStatus(),
                rota.getDataRota(),
                rota.getFinalizadaEm(),
                com.andregattermeyer.api_frota.mapper.Mapper.toVeiculoResponseDTO(rota.getVeiculo()),
                rota.getCriadaEm(),
                paradas.stream().map(PontoParadaResponseDTO::from).toList(),
                registros);
    }

    @Transactional
    public RotaMotoristaResponseDTO finalizarRotaAtual(Long veiculoId, String username) {
        var usuario = usuarioRepository.findWithMotoristaByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Conta de motorista não encontrada."));
        if (usuario.getMotorista() == null) {
            throw new AccessDeniedException("A conta não está vinculada a um motorista.");
        }
        var motorista = usuario.getMotorista();
        var vinculo = vinculoRepository.findByVeiculoIdAndMotoristaIdAndAtivoTrue(veiculoId, motorista.getId())
                .orElseThrow(() -> new AccessDeniedException("Este veículo não está atribuído ao motorista."));
        if (rotaRepository.existsByMotorista_IdAndDataRota(motorista.getId(), LocalDate.now())) {
            throw new BadRequestException("Já existe uma rota diária programada para hoje.");
        }
        if (rotaRepository.existsByVeiculo_IdAndDataRota(veiculoId, LocalDate.now())) {
            throw new BadRequestException("Este veículo já está atribuído a outra rota diária hoje.");
        }

        var agora = LocalDateTime.now();
        var rota = new RotaDiaria();
        rota.setMotorista(motorista);
        rota.setVeiculo(vinculo.getVeiculo());
        rota.setDataRota(LocalDate.now());
        rota.setStatus(StatusRotaDiaria.FINALIZADA);
        rota.setCriadaEm(agora);
        rota.setFinalizadaEm(agora);
        rota = rotaRepository.save(rota);

        var paradasOriginais = paradaRepository.findByVeiculoIdAndRotaDiariaIsNullOrderByOrdemAscIdAsc(veiculoId);
        List<PontoParada> paradas = new java.util.ArrayList<>();
        var paradasPorId = new java.util.HashMap<Long, PontoParada>();
        for (PontoParada original : paradasOriginais) {
            PontoParada copia = new PontoParada();
            copia.setDescricao(original.getDescricao());
            copia.setNomeEstabelecimento(original.getNomeEstabelecimento());
            copia.setEndereco(original.getEndereco());
            copia.setTipo(original.getTipo());
            copia.setLatitude(original.getLatitude());
            copia.setLongitude(original.getLongitude());
            copia.setOrdem(original.getOrdem());
            copia.setVeiculo(vinculo.getVeiculo());
            copia.setRotaDiaria(rota);
            paradas.add(copia);
        }
        paradas = paradaRepository.saveAll(paradas);

        for (int indice = 0; indice < paradasOriginais.size(); indice++) {
            paradasPorId.put(paradasOriginais.get(indice).getId(), paradas.get(indice));
        }
        var registrosExistentes = paradasOriginais.isEmpty()
                ? List.<RegistroParada>of()
                : registroRepository.findByPontoParada_IdInOrderByRegistradoEmDesc(
                        paradasOriginais.stream().map(PontoParada::getId).toList());
        var registrosDeHoje = registrosExistentes.stream()
                .filter(registro -> registro.getRegistradoEm().toLocalDate().equals(LocalDate.now()))
                .toList();
        registrosDeHoje.forEach(registro -> registro.setPontoParada(
                paradasPorId.get(registro.getPontoParada().getId())));
        registroRepository.saveAll(registrosDeHoje);
        return respostaMotorista(rota, paradas);
    }

    private RotaDiariaResponseDTO respostaAdmin(RotaDiaria rota, List<PontoParada> paradas) {
        return RotaDiariaResponseDTO.from(
                rota,
                paradas.stream().map(PontoParadaResponseDTO::from).toList(),
                registrosDasParadas(paradas));
    }

    private RotaMotoristaResponseDTO respostaMotorista(RotaDiaria rota, List<PontoParada> paradas) {
        return new RotaMotoristaResponseDTO(
                rota.getId(),
                rota.getStatus(),
                rota.getDataRota(),
                rota.getFinalizadaEm(),
                com.andregattermeyer.api_frota.mapper.Mapper.toVeiculoResponseDTO(rota.getVeiculo()),
                rota.getCriadaEm(),
                paradas.stream().map(PontoParadaResponseDTO::from).toList(),
                registrosDasParadas(paradas));
    }

    private List<RegistroParadaResponseDTO> registrosDasParadas(List<PontoParada> paradas) {
        if (paradas.isEmpty()) {
            return List.of();
        }
        return registroRepository.findByPontoParada_IdInOrderByRegistradoEmDesc(
                        paradas.stream().map(PontoParada::getId).toList()).stream()
                .map(RegistroParadaResponseDTO::from)
                .toList();
    }

    private boolean mesmaParada(PontoParada parada, ParadaRotaDiariaAtualizacaoRequestDTO dados) {
        return java.util.Objects.equals(parada.getDescricao(), dados.descricao().trim())
                && java.util.Objects.equals(parada.getNomeEstabelecimento(), limpar(dados.nomeEstabelecimento()))
                && java.util.Objects.equals(parada.getEndereco(), limpar(dados.endereco()))
                && parada.getTipo() == dados.tipo()
                && java.util.Objects.equals(parada.getLatitude(), dados.latitude())
                && java.util.Objects.equals(parada.getLongitude(), dados.longitude());
    }

    private String limpar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
