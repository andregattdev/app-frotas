package com.andregattermeyer.api_frota.config;

import java.util.Set;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.andregattermeyer.api_frota.auth.Role;
import com.andregattermeyer.api_frota.auth.Usuario;
import com.andregattermeyer.api_frota.repository.UsuarioRepository;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.username:}")
    private String username;

    @Value("${app.bootstrap-admin.email:}")
    private String email;

    @Value("${app.bootstrap-admin.password:}")
    private String password;

    public AdminBootstrapRunner(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean anyValueSet = notBlank(username) || notBlank(email) || notBlank(password);
        if (!anyValueSet) {
            return;
        }
        if (!notBlank(username) || !notBlank(email) || password == null || password.length() < 12
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                    "Configure BOOTSTRAP_ADMIN_USERNAME, BOOTSTRAP_ADMIN_EMAIL and a 12-72 byte BOOTSTRAP_ADMIN_PASSWORD together.");
        }

        String normalizedUsername = username.trim();
        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        Usuario existingByEmail = usuarioRepository.findByEmail(normalizedEmail).orElse(null);
        Usuario existingByUsername = usuarioRepository.findByUsername(normalizedUsername).orElse(null);
        if (existingByEmail != null) {
            if (existingByUsername != null && !existingByUsername.getId().equals(existingByEmail.getId())) {
                throw new IllegalStateException("The configured bootstrap username and email belong to different accounts.");
            }
            Set<Role> roles = new HashSet<>(existingByEmail.getRoles());
            roles.add(Role.ADMIN);
            existingByEmail.setRoles(roles);
            usuarioRepository.save(existingByEmail);
            return;
        }
        if (existingByUsername != null) {
            throw new IllegalStateException("The configured bootstrap username is already registered with a different email.");
        }

        Usuario admin = new Usuario();
        admin.setUsername(normalizedUsername);
        admin.setEmail(normalizedEmail);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRoles(Set.of(Role.ADMIN));
        usuarioRepository.save(admin);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
