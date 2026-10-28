package com.andregattermeyer.api_frota.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.andregattermeyer.api_frota.auth.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Métodos de busca por campos únicos (muito usados em autenticação)
    Optional<Usuario> findByUsername(String username);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "motorista")
    Optional<Usuario> findWithMotoristaByUsername(String username);

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByMotoristaId(Long motoristaId);

    boolean existsByMotoristaId(Long motoristaId);

    // Verificações de existência (mais performáticas que count() ou findBy... != null)
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Opcional: se quiser buscar ignorando case (útil em alguns sistemas)
    // Optional<Usuario> findByUsernameIgnoreCase(String username);

    // Opcional: busca por username OU email (útil no login)
    Optional<Usuario> findByUsernameOrEmail(String username, String email);

    // Exemplos de métodos que você pode adicionar depois, conforme necessidade:
    // List<Usuario> findByAtivoTrue();
    // List<Usuario> findByRolesContaining(String role);
    // @Query("SELECT u FROM Usuario u WHERE u.email LIKE %:dominio")
    // List<Usuario> findByEmailContainingDominio(@Param("dominio") String dominio);


}
