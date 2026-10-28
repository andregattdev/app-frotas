package com.andregattermeyer.api_frota.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.andregattermeyer.api_frota.auth.Usuario;
import com.andregattermeyer.api_frota.dto.response.PontoParadaResponseDTO;
import com.andregattermeyer.api_frota.dto.response.RotaMotoristaResponseDTO;
import com.andregattermeyer.api_frota.mapper.Mapper;
import com.andregattermeyer.api_frota.repository.PontoParadaRepository;
import com.andregattermeyer.api_frota.repository.UsuarioRepository;
import com.andregattermeyer.api_frota.repository.VinculoMotoristaVeiculoRepository;

@Service
public class RotaMotoristaService {

    private final UsuarioRepository usuarioRepository;
    private final VinculoMotoristaVeiculoRepository vinculoRepository;
    private final PontoParadaRepository pontoParadaRepository;
    private final RegistroParadaService registroParadaService;

    public RotaMotoristaService(
            UsuarioRepository usuarioRepository,
            VinculoMotoristaVeiculoRepository vinculoRepository,
            PontoParadaRepository pontoParadaRepository,
            RegistroParadaService registroParadaService) {
        this.usuarioRepository = usuarioRepository;
        this.vinculoRepository = vinculoRepository;
        this.pontoParadaRepository = pontoParadaRepository;
        this.registroParadaService = registroParadaService;
    }

    @Transactional(readOnly = true)
    public List<RotaMotoristaResponseDTO> listarRotasDoMotorista(String username) {
        Usuario usuario = usuarioRepository.findWithMotoristaByUsername(username).orElse(null);
        if (usuario == null || usuario.getMotorista() == null) {
            return List.of();
        }

        Long motoristaId = usuario.getMotorista().getId();
        return vinculoRepository.findByMotoristaIdAndAtivoTrueOrderByDataHoraInicioDesc(motoristaId).stream()
                .map(vinculo -> {
                    var paradas = pontoParadaRepository.findByVeiculoIdAndRotaDiariaIsNullOrderByOrdemAscIdAsc(
                            vinculo.getVeiculo().getId());
                    return new RotaMotoristaResponseDTO(
                            null,
                            null,
                            null,
                            null,
                            Mapper.toVeiculoResponseDTO(vinculo.getVeiculo()),
                            vinculo.getDataHoraInicio(),
                            paradas.stream().map(PontoParadaResponseDTO::from).toList(),
                            registroParadaService.listarPorParadas(
                                    paradas.stream().map(parada -> parada.getId()).toList()));
                })
                .toList();
    }
}
