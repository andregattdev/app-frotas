package com.andregattermeyer.api_frota.config;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.andregattermeyer.api_frota.auth.Usuario;
import com.andregattermeyer.api_frota.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioDetailsServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioDetailsServiceImpl userDetailsService;

    @Test
    void loadsAccountByEmailIgnoringEmailCaseAndSurroundingWhitespace() {
        Usuario usuario = new Usuario();
        when(usuarioRepository.findByUsernameOrEmail("Gestor@Email.com", "gestor@email.com"))
                .thenReturn(Optional.of(usuario));

        assertSame(usuario, userDetailsService.loadUserByUsername(" Gestor@Email.com "));
        verify(usuarioRepository).findByUsernameOrEmail("Gestor@Email.com", "gestor@email.com");
    }
}
