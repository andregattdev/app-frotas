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

import com.andregattermeyer.api_frota.dto.request.OficinaRequestDTO;
import com.andregattermeyer.api_frota.dto.response.OficinaResponseDTO;
import com.andregattermeyer.api_frota.dto.response.PagedResponse;
import com.andregattermeyer.api_frota.service.OficinaService;
import com.andregattermeyer.api_frota.config.PaginationSupport;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@Validated
@RequestMapping("/api/oficinas")
public class OficinaController {

    @Autowired
    private OficinaService oficinaService;

    @GetMapping
    public Object listarTodas(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) @Min(0) @Max(1000000) Integer page,
            @RequestParam(required = false) @Min(1) @Max(100) Integer size) {
        if (!PaginationSupport.requested(page, size)) {
            return oficinaService.listarTodas(busca);
        }
        return PagedResponse.from(oficinaService.listarPagina(busca, PaginationSupport.pageable(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OficinaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(oficinaService.buscarPorId(id));
    }

    @GetMapping("/nome/{nome}")
    public ResponseEntity<OficinaResponseDTO> buscarPorNomeOficina(@PathVariable String nome) {
        return ResponseEntity.ok(oficinaService.buscarPorNomeOficina(nome));
    }

    @GetMapping("/nomeParcial/{termo}")
    public List<OficinaResponseDTO> buscarPorNomeParcial(@PathVariable String termo) {
        return oficinaService.buscarPorNomeParcial(termo);
    }

    @GetMapping("/especialidade/{especialidade}")
    public List<OficinaResponseDTO> buscarPorEspecialidade(@PathVariable String especialidade) {
        return oficinaService.buscarPorEspecialidade(especialidade);
    }

    @GetMapping("/endereco/{termo}")
    public List<OficinaResponseDTO> procurarPorEndereco(@PathVariable String termo) {
        return oficinaService.procurarPorEndereco(termo);
    }

    @PostMapping
    public ResponseEntity<OficinaResponseDTO> salvar(@RequestBody @Valid OficinaRequestDTO dto) {
        return ResponseEntity.ok(oficinaService.salvar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OficinaResponseDTO> atualizar(@PathVariable Long id,
                                                         @RequestBody @Valid OficinaRequestDTO dto) {
        return ResponseEntity.ok(oficinaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        oficinaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
