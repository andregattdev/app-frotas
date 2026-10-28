package com.andregattermeyer.api_frota.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.andregattermeyer.api_frota.dto.request.PontoParadaRequestDTO;
import com.andregattermeyer.api_frota.dto.response.PontoParadaResponseDTO;
import com.andregattermeyer.api_frota.service.PontoParadaService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@RestController
@Validated
@RequestMapping("/api/veiculos/{veiculoId}/paradas")
public class PontoParadaController {

    private final PontoParadaService pontoParadaService;

    public PontoParadaController(PontoParadaService pontoParadaService) {
        this.pontoParadaService = pontoParadaService;
    }

    @GetMapping
    public List<PontoParadaResponseDTO> listar(@PathVariable Long veiculoId) {
        return pontoParadaService.listarPorVeiculo(veiculoId);
    }

    @PostMapping
    public ResponseEntity<PontoParadaResponseDTO> criar(
            @PathVariable Long veiculoId,
            @RequestBody @Valid PontoParadaRequestDTO dto) {
        return ResponseEntity.ok(pontoParadaService.criar(veiculoId, dto));
    }

    @PutMapping("/ordem")
    public List<PontoParadaResponseDTO> reordenar(
            @PathVariable Long veiculoId,
            @RequestBody @NotEmpty List<@NotNull Long> ids) {
        return pontoParadaService.reordenar(veiculoId, ids);
    }

    @PutMapping("/{pontoId}")
    public ResponseEntity<PontoParadaResponseDTO> atualizar(
            @PathVariable Long veiculoId,
            @PathVariable Long pontoId,
            @RequestBody @Valid PontoParadaRequestDTO dto) {
        return ResponseEntity.ok(pontoParadaService.atualizar(veiculoId, pontoId, dto));
    }

    @DeleteMapping("/{pontoId}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long veiculoId,
            @PathVariable Long pontoId) {
        pontoParadaService.excluir(veiculoId, pontoId);
        return ResponseEntity.noContent().build();
    }
}
