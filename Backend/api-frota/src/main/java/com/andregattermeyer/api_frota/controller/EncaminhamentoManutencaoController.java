package com.andregattermeyer.api_frota.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.andregattermeyer.api_frota.dto.request.CriarEncaminhamentoManutencaoRequest;
import com.andregattermeyer.api_frota.dto.response.EncaminhamentoManutencaoResponse;
import com.andregattermeyer.api_frota.service.EncaminhamentoManutencaoService;

import jakarta.validation.Valid;

@RestController
public class EncaminhamentoManutencaoController {

    private final EncaminhamentoManutencaoService service;

    public EncaminhamentoManutencaoController(EncaminhamentoManutencaoService service) {
        this.service = service;
    }

    @GetMapping("/api/assistente/encaminhamentos")
    public List<EncaminhamentoManutencaoResponse> listarAtivos() {
        return service.listarAtivos();
    }

    @PostMapping("/api/assistente/encaminhamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public EncaminhamentoManutencaoResponse criar(
            @RequestBody @Valid CriarEncaminhamentoManutencaoRequest request) {
        return service.criar(request);
    }

    @DeleteMapping("/api/assistente/encaminhamentos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void concluir(@PathVariable Long id) {
        service.concluir(id);
    }

    @GetMapping("/api/rotas/minhas/encaminhamentos")
    public List<EncaminhamentoManutencaoResponse> listarDoMotorista(
            @AuthenticationPrincipal UserDetails usuario) {
        return service.listarDoMotorista(usuario.getUsername());
    }
}
