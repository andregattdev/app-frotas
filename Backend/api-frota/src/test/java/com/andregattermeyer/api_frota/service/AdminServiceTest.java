package com.andregattermeyer.api_frota.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.andregattermeyer.api_frota.auth.Role;
import com.andregattermeyer.api_frota.auth.Usuario;
import com.andregattermeyer.api_frota.auth.dto.ContaAssistenteResponse;
import com.andregattermeyer.api_frota.auth.dto.CriarContaAssistenteRequest;
import com.andregattermeyer.api_frota.exception.BadRequestException;
import com.andregattermeyer.api_frota.repository.MotoristaRepository;
import com.andregattermeyer.api_frota.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private MotoristaRepository motoristaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private VeiculoService veiculoService;

    @InjectMocks
    private AdminService adminService;

    @Test
    void refusesToAssignVehicleToAccountWithoutDriverRole() {
        Usuario account = new Usuario("operator", "operator@example.com", "encoded", Set.of(Role.USER));
        when(usuarioRepository.findByMotoristaId(42L)).thenReturn(Optional.of(account));

        assertThrows(BadRequestException.class, () -> adminService.atribuirMotorista(7L, 42L));
        verifyNoInteractions(veiculoService);
    }

    @Test
    void createsAnAccountWithOnlyTheFleetAssistantRole() {
        CriarContaAssistenteRequest request = new CriarContaAssistenteRequest();
        request.setUsername("assistente");
        request.setEmail("assistente@example.com");
        request.setPassword("uma-senha-segura-123");
        when(usuarioRepository.existsByUsername("assistente")).thenReturn(false);
        when(usuarioRepository.existsByEmail("assistente@example.com")).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded");
        when(usuarioRepository.save(org.mockito.ArgumentMatchers.any(Usuario.class)))
                .thenAnswer(invocation -> {
                    Usuario usuario = invocation.getArgument(0);
                    usuario.setId(9L);
                    return usuario;
                });

        ContaAssistenteResponse response = adminService.criarContaAssistente(request);

        assertEquals("ASSISTENTE_FROTA", response.role());
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals(Set.of(Role.ASSISTENTE_FROTA), captor.getValue().getRoles());
        assertNull(captor.getValue().getMotorista());
    }
}
