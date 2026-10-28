package com.andregattermeyer.api_frota.auth;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.andregattermeyer.api_frota.auth.dto.AlterarSenhaRequest;
import com.andregattermeyer.api_frota.auth.dto.AuthenticationResponse;
import com.andregattermeyer.api_frota.auth.dto.LoginRequest;
import com.andregattermeyer.api_frota.auth.dto.RegistrarRequest;
import com.andregattermeyer.api_frota.exception.BadRequestException;
import com.andregattermeyer.api_frota.repository.UsuarioRepository;

@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService, AuthenticationManager authenticationManager) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthenticationResponse registrar(RegistrarRequest request) {
        validarTamanhoSenha(request.getPassword());
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
        usuario.setRoles(Set.of(Role.USER));

        usuarioRepository.save(usuario);

        String token = jwtService.generateToken(usuario);
        return AuthenticationResponse.from(usuario, token);
    }

    public AuthenticationResponse login(LoginRequest request) {
        validarTamanhoSenha(request.getPassword());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        Usuario usuario = (Usuario) authentication.getPrincipal();
        String token = jwtService.generateToken(usuario);
        return AuthenticationResponse.from(usuario, token);
    }

    @Transactional
    public void alterarSenha(String username, AlterarSenhaRequest request) {
        validarTamanhoSenha(request.getNovaSenha());
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Conta de usuário não encontrada."));
        if (!passwordEncoder.matches(request.getSenhaAtual(), usuario.getPassword())) {
            throw new BadRequestException("A senha atual está incorreta.");
        }
        if (passwordEncoder.matches(request.getNovaSenha(), usuario.getPassword())) {
            throw new BadRequestException("A nova senha deve ser diferente da senha atual.");
        }
        usuario.setPassword(passwordEncoder.encode(request.getNovaSenha()));
        usuarioRepository.save(usuario);
    }

    private void validarTamanhoSenha(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadRequestException("A senha não pode exceder 72 bytes em UTF-8.");
        }
    }
}
