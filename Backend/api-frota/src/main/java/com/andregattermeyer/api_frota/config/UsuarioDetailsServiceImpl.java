package com.andregattermeyer.api_frota.config;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.andregattermeyer.api_frota.auth.Usuario;
import com.andregattermeyer.api_frota.repository.UsuarioRepository;



@Service
public class UsuarioDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String identifier = username.trim();
        Usuario usuario = usuarioRepository.findByUsernameOrEmail(identifier, identifier.toLowerCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

        return usuario;
    }
}
