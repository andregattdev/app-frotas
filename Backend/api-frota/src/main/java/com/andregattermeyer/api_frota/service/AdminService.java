package com.andregattermeyer.api_frota.service;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.andregattermeyer.api_frota.auth.Role;
import com.andregattermeyer.api_frota.auth.Usuario;
import com.andregattermeyer.api_frota.auth.dto.ContaMotoristaResponse;
import com.andregattermeyer.api_frota.auth.dto.ContaAssistenteResponse;
import com.andregattermeyer.api_frota.auth.dto.CriarContaAssistenteRequest;
import com.andregattermeyer.api_frota.auth.dto.CriarContaMotoristaRequest;
import com.andregattermeyer.api_frota.dto.response.VeiculoResponseDTO;
import com.andregattermeyer.api_frota.dto.response.MotoristaResponseDTO;
import com.andregattermeyer.api_frota.entity.Motorista;
import com.andregattermeyer.api_frota.exception.BadRequestException;
import com.andregattermeyer.api_frota.exception.ResourceNotFoundException;
import com.andregattermeyer.api_frota.repository.MotoristaRepository;
import com.andregattermeyer.api_frota.repository.UsuarioRepository;
import com.andregattermeyer.api_frota.mapper.Mapper;

import java.util.List;

@Service
public class AdminService {

    private final MotoristaRepository motoristaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final VeiculoService veiculoService;

    public AdminService(
            MotoristaRepository motoristaRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            VeiculoService veiculoService) {
        this.motoristaRepository = motoristaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.veiculoService = veiculoService;
    }

    @Transactional
    public ContaMotoristaResponse criarContaMotorista(Long motoristaId, CriarContaMotoristaRequest request) {
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadRequestException("A senha não pode exceder 72 bytes em UTF-8.");
        }
        Motorista motorista = motoristaRepository.findById(motoristaId)
                .orElseThrow(() -> new ResourceNotFoundException("Motorista não encontrado."));
        if (usuarioRepository.existsByMotoristaId(motoristaId)) {
            throw new BadRequestException("Este motorista já possui uma conta de acesso.");
        }

        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();
        if (usuarioRepository.existsByUsername(username)) {
            throw new BadRequestException("Nome de usuário já existe.");
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new BadRequestException("E-mail já cadastrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setRoles(Set.of(Role.MOTORISTA));
        usuario.setMotorista(motorista);
        usuarioRepository.save(usuario);
        return new ContaMotoristaResponse(motorista.getId(), motorista.getNome(), username, email, Role.MOTORISTA.name());
    }

    @Transactional
    public ContaAssistenteResponse criarContaAssistente(CriarContaAssistenteRequest request) {
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadRequestException("A senha não pode exceder 72 bytes em UTF-8.");
        }

        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        if (usuarioRepository.existsByUsername(username)) {
            throw new BadRequestException("Nome de usuário já existe.");
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new BadRequestException("E-mail já cadastrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setRoles(Set.of(Role.ASSISTENTE_FROTA));
        Usuario salvo = usuarioRepository.save(usuario);
        return new ContaAssistenteResponse(
                salvo.getId(), username, email, Role.ASSISTENTE_FROTA.name());
    }

    @Transactional
    public VeiculoResponseDTO atribuirMotorista(Long veiculoId, Long motoristaId) {
        Usuario usuario = usuarioRepository.findByMotoristaId(motoristaId)
                .orElseThrow(() -> new BadRequestException("O motorista precisa ter uma conta vinculada antes da atribuição."));
        if (!usuario.getRoles().contains(Role.MOTORISTA)) {
            throw new BadRequestException("A conta vinculada não possui o perfil de motorista.");
        }
        return veiculoService.vincularMotorista(veiculoId, motoristaId);
    }

    @Transactional(readOnly = true)
    public List<MotoristaResponseDTO> listarMotoristasComConta() {
        return Mapper.toMotoristaResponseDTOList(usuarioRepository.findAll().stream()
                .filter(usuario -> usuario.getRoles().contains(Role.MOTORISTA))
                .map(usuario -> usuario.getMotorista())
                .filter(motorista -> motorista != null)
                .toList());
    }
}
