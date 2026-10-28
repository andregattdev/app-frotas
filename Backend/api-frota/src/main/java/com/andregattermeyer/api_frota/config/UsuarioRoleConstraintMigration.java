package com.andregattermeyer.api_frota.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UsuarioRoleConstraintMigration implements ApplicationRunner {

    private static final String ROLE_CONSTRAINT = """
            CHECK (role IN (
                'SUPER_ADMIN',
                'ADMIN',
                'ASSISTENTE_FROTA',
                'GESTOR_FROTA',
                'OPERADOR',
                'MOTORISTA',
                'MECANICO',
                'FINANCEIRO',
                'USER'
            ))
            """;

    private final JdbcTemplate jdbcTemplate;

    public UsuarioRoleConstraintMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE usuario_roles DROP CONSTRAINT IF EXISTS usuario_roles_role_check");
        jdbcTemplate.execute(
                "ALTER TABLE usuario_roles ADD CONSTRAINT usuario_roles_role_check " + ROLE_CONSTRAINT);
    }
}
