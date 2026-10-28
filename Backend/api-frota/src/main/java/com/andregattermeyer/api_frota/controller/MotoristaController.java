package com.andregattermeyer.api_frota.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import com.andregattermeyer.api_frota.dto.request.MotoristaRequestDTO;
import com.andregattermeyer.api_frota.dto.response.MotoristaResponseDTO;
import com.andregattermeyer.api_frota.dto.response.PagedResponse;
import com.andregattermeyer.api_frota.service.MotoristaService;
import com.andregattermeyer.api_frota.config.PaginationSupport;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@Validated
@RequestMapping("/api/motoristas")
public class MotoristaController {

    @Autowired
    private MotoristaService motoristaService;

    @GetMapping
    public Object listarTodos(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) @Min(0) @Max(1000000) Integer page,
            @RequestParam(required = false) @Min(1) @Max(100) Integer size) {
        if (!PaginationSupport.requested(page, size)) {
            return motoristaService.listarTodos(busca);
        }
        return PagedResponse.from(motoristaService.listarPagina(busca, PaginationSupport.pageable(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MotoristaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(motoristaService.buscarPorId(id));
    }

    @GetMapping("/nome/{nome}")
    public List<MotoristaResponseDTO> buscarPorNome(@PathVariable String nome) {
        return motoristaService.buscarPorNome(nome);
    }

    @GetMapping("/nomeIgnoreCase/{nome}")
    public List<MotoristaResponseDTO> buscarPorNomeIgnoreCase(@PathVariable String nome) {
        return motoristaService.buscarPorNomeIgnoreCase(nome);
    }

    @GetMapping("/nomeParcial/{nome}")
    public List<MotoristaResponseDTO> buscarPorNomeParcial(@PathVariable String nome) {
        return motoristaService.buscarPorNomeParcial(nome);
    }

    @GetMapping("/cnh/{cnh}")
    public ResponseEntity<MotoristaResponseDTO> buscarPorCnh(@PathVariable String cnh) {
        return ResponseEntity.ok(motoristaService.buscarPorCnh(cnh));
    }

    @GetMapping("/categoria/{categoria}")
    public List<MotoristaResponseDTO> buscarPorCategoria(@PathVariable String categoria) {
        return motoristaService.buscarPorCategoria(categoria);
    }

    @GetMapping("/cnh-vencida")
    public List<MotoristaResponseDTO> buscarMotoristasComCnhVencida() {
        return motoristaService.buscarMotoristasComCnhVencida();
    }

    @PostMapping
    public ResponseEntity<MotoristaResponseDTO> salvar(@RequestBody @Valid MotoristaRequestDTO dto) {
        return ResponseEntity.ok(motoristaService.salvar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MotoristaResponseDTO> atualizar(@PathVariable Long id,
                                                          @RequestBody @Valid MotoristaRequestDTO dto) {
        return ResponseEntity.ok(motoristaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        motoristaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
