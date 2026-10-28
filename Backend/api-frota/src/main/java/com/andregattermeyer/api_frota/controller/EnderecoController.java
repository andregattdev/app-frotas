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

import com.andregattermeyer.api_frota.dto.request.EnderecoRequestDTO;
import com.andregattermeyer.api_frota.dto.response.EnderecoResponseDTO;
import com.andregattermeyer.api_frota.dto.response.PagedResponse;
import com.andregattermeyer.api_frota.dto.response.ViaCepResponseDTO;
import com.andregattermeyer.api_frota.service.EnderecoService;
import com.andregattermeyer.api_frota.service.ViaCepService;
import com.andregattermeyer.api_frota.config.PaginationSupport;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@Validated
@RequestMapping("/api/enderecos")
public class EnderecoController {

    @Autowired
    private EnderecoService enderecoService;

    @Autowired
    private ViaCepService viaCepService;

    @GetMapping
    public Object listarTodos(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) @Min(0) @Max(1000000) Integer page,
            @RequestParam(required = false) @Min(1) @Max(100) Integer size) {
        if (!PaginationSupport.requested(page, size)) {
            return enderecoService.listarTodos(busca);
        }
        return PagedResponse.from(enderecoService.listarPagina(busca, PaginationSupport.pageable(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnderecoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(enderecoService.buscarPorId(id));
    }

    @GetMapping("/consulta-cep/{cep}")
    public ResponseEntity<ViaCepResponseDTO> consultarCep(@PathVariable String cep) {
        return ResponseEntity.ok(viaCepService.consultar(cep));
    }

    @GetMapping("/cep/{cep}")
    public ResponseEntity<EnderecoResponseDTO> buscarPorCep(@PathVariable String cep) {
        return ResponseEntity.ok(enderecoService.buscarPorCep(cep));
    }

    @GetMapping("/cidade/{cidade}")
    public List<EnderecoResponseDTO> buscarPorCidade(@PathVariable String cidade) {
        return enderecoService.buscarPorCidade(cidade);
    }

    @GetMapping("/estado/{estado}")
    public List<EnderecoResponseDTO> buscarPorEstado(@PathVariable String estado) {
        return enderecoService.buscarPorEstado(estado);
    }

    @GetMapping("/bairro/{bairro}")
    public List<EnderecoResponseDTO> buscarPorBairro(@PathVariable String bairro) {
        return enderecoService.buscarPorBairro(bairro);
    }

    @PostMapping
    public ResponseEntity<EnderecoResponseDTO> salvar(@RequestBody @Valid EnderecoRequestDTO dto) {
        return ResponseEntity.ok(enderecoService.salvar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EnderecoResponseDTO> atualizar(@PathVariable Long id,
                                                         @RequestBody @Valid EnderecoRequestDTO dto) {
        return ResponseEntity.ok(enderecoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        enderecoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
