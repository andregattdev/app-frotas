package com.andregattermeyer.api_frota.auth;

public enum Role {

    SUPER_ADMIN, // Controle total do sistema
    ADMIN, // Gerencia a empresa/frota
    ASSISTENTE_FROTA, // Gerencia veículos, checklists, oficinas e encaminhamentos
    GESTOR_FROTA, // Aprova manutenções, visualiza relatórios
    OPERADOR, // Cadastra veículos e abastecimentos
    MOTORISTA, // Consulta seus veículos e viagens
    MECANICO, // Gerencia manutenções
    FINANCEIRO, // Custos, multas e relatórios financeiros
    USER // Acesso básico

}
