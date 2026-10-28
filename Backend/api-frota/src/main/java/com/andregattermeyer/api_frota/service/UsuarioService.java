package com.andregattermeyer.api_frota.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.andregattermeyer.api_frota.auth.Usuario;
import com.andregattermeyer.api_frota.repository.UsuarioRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Usuario criarUsuario(Usuario usuario) {
       
        if (usuarioRepository.existsByUsername(usuario.getUsername())) {
            throw new IllegalArgumentException("Username já está em uso");
        }
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new IllegalArgumentException("E-mail já está em uso");
        }

        // Normalmente a senha deveria ser criptografada aqui
        // usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

        return usuarioRepository.save(usuario);
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    public Usuario buscarPorIdOuLancarExcecao(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com id: " + id));
    }

    @Transactional
    public Usuario atualizar(Long id, Usuario dadosAtualizados) {
        Usuario usuario = buscarPorIdOuLancarExcecao(id);

        // Atualiza apenas os campos enviados (evita sobrescrever senha com null, por exemplo)
        if (dadosAtualizados.getUsername() != null) {
            if (!dadosAtualizados.getUsername().equals(usuario.getUsername()) &&
                usuarioRepository.existsByUsername(dadosAtualizados.getUsername())) {
                throw new IllegalArgumentException("Username já está em uso");
            }
            usuario.setUsername(dadosAtualizados.getUsername());
        }

        if (dadosAtualizados.getEmail() != null) {
            if (!dadosAtualizados.getEmail().equals(usuario.getEmail()) &&
                usuarioRepository.existsByEmail(dadosAtualizados.getEmail())) {
                throw new IllegalArgumentException("E-mail já está em uso");
            }
            usuario.setEmail(dadosAtualizados.getEmail());
        }

        // Regra comum: NÃO atualizar senha diretamente por este endpoint
        // Senha deve ter endpoint separado com validação de senha atual

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void excluir(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new EntityNotFoundException("Usuário não encontrado com id: " + id);
        }
        usuarioRepository.deleteById(id);
    }

    // Métodos extras úteis (opcional)
    public boolean existePorUsername(String username) {
        return usuarioRepository.existsByUsername(username);
    }

    public boolean existePorEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }


}
