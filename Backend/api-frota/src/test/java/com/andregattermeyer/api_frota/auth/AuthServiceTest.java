package com.andregattermeyer.api_frota.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.andregattermeyer.api_frota.auth.dto.RegistrarRequest;
import com.andregattermeyer.api_frota.auth.dto.AlterarSenhaRequest;
import com.andregattermeyer.api_frota.exception.BadRequestException;
import com.andregattermeyer.api_frota.repository.UsuarioRepository;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    void publicRegistrationAlwaysCreatesOnlyBasicUserRole() {
        RegistrarRequest request = new RegistrarRequest();
        request.setUsername("  operador  ");
        request.setEmail(" OPERADOR@EXAMPLE.COM ");
        request.setPassword("senha-de-teste-com-12");
        when(passwordEncoder.encode(request.getPassword())).thenReturn("bcrypt-hash");
        when(jwtService.generateToken(any(Usuario.class))).thenReturn("jwt-token");

        authService.registrar(request);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario usuario = usuarioCaptor.getValue();
        assertEquals("operador", usuario.getUsername());
        assertEquals("operador@example.com", usuario.getEmail());
        assertEquals("bcrypt-hash", usuario.getPassword());
        assertEquals(Set.of(Role.USER), usuario.getRoles());
    }

    @Test
    void publicRegistrationRejectsPasswordLongerThanBcryptUtf8Limit() {
        RegistrarRequest request = new RegistrarRequest();
        request.setUsername("operador");
        request.setEmail("operador@example.com");
        request.setPassword("á".repeat(37));

        assertThrows(BadRequestException.class, () -> authService.registrar(request));
        verifyNoInteractions(usuarioRepository, passwordEncoder, jwtService);
    }

    @Test
    void changesPasswordOnlyAfterVerifyingCurrentPassword() {
        Usuario usuario = new Usuario("motorista", "motorista@example.com", "senha-antiga-hash", Set.of(Role.MOTORISTA));
        AlterarSenhaRequest request = new AlterarSenhaRequest();
        request.setSenhaAtual("senha-atual-valida");
        request.setNovaSenha("nova-senha-valida-12");
        when(usuarioRepository.findByUsername("motorista")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(request.getSenhaAtual(), usuario.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(request.getNovaSenha(), usuario.getPassword())).thenReturn(false);
        when(passwordEncoder.encode(request.getNovaSenha())).thenReturn("nova-senha-hash");

        authService.alterarSenha("motorista", request);

        assertEquals("nova-senha-hash", usuario.getPassword());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void refusesPasswordChangeWhenCurrentPasswordIsIncorrect() {
        Usuario usuario = new Usuario("motorista", "motorista@example.com", "senha-antiga-hash", Set.of(Role.MOTORISTA));
        AlterarSenhaRequest request = new AlterarSenhaRequest();
        request.setSenhaAtual("senha-incorreta");
        request.setNovaSenha("nova-senha-valida-12");
        when(usuarioRepository.findByUsername("motorista")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(request.getSenhaAtual(), usuario.getPassword())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.alterarSenha("motorista", request));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
}
