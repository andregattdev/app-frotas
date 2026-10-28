package com.andregattermeyer.api_frota.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.andregattermeyer.api_frota.auth.Role;
import com.andregattermeyer.api_frota.auth.Usuario;
import com.andregattermeyer.api_frota.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminBootstrapRunner runner;

    @Test
    void promotesConfiguredExistingEmailToAdminWithoutChangingPassword() throws Exception {
        ReflectionTestUtils.setField(runner, "username", "gestor");
        ReflectionTestUtils.setField(runner, "email", "GESTOR@EMAIL.COM");
        ReflectionTestUtils.setField(runner, "password", "local-test-password");

        Usuario usuario = new Usuario("gestor", "gestor@email.com", "existing-hash", Set.of(Role.USER));
        usuario.setId(42L);
        when(usuarioRepository.findByEmail("gestor@email.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByUsername("gestor")).thenReturn(Optional.of(usuario));

        runner.run(new DefaultApplicationArguments(new String[0]));

        assertEquals(Set.of(Role.USER, Role.ADMIN), usuario.getRoles());
        assertEquals("existing-hash", usuario.getPassword());
        verify(usuarioRepository).save(usuario);
    }
}
