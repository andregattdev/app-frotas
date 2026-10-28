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

import com.andregattermeyer.api_frota.dto.request.VeiculoRequestDTO;
import com.andregattermeyer.api_frota.dto.response.MotoristaResponseDTO;
import com.andregattermeyer.api_frota.dto.response.PagedResponse;
import com.andregattermeyer.api_frota.dto.response.VeiculoResponseDTO;
import com.andregattermeyer.api_frota.dto.response.VinculoMotoristaVeiculoResponseDTO;
import com.andregattermeyer.api_frota.entity.Tipo;
import com.andregattermeyer.api_frota.service.VeiculoService;
import com.andregattermeyer.api_frota.service.VinculoMotoristaVeiculoService;
import com.andregattermeyer.api_frota.config.PaginationSupport;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@Validated
@RequestMapping("/api/veiculos")
public class VeiculoController {

    @Autowired
    private VeiculoService veiculoService;

    @Autowired
    private VinculoMotoristaVeiculoService vinculoService;

    @GetMapping
    public Object listarTodos(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) @Min(0) @Max(1000000) Integer page,
            @RequestParam(required = false) @Min(1) @Max(100) Integer size) {
        if (!PaginationSupport.requested(page, size)) {
            return veiculoService.listarTodos(busca);
        }
        return PagedResponse.from(veiculoService.listarPagina(busca, PaginationSupport.pageable(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VeiculoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(veiculoService.buscarPorId(id));
    }

    @GetMapping("/placa/{placa}")
    public ResponseEntity<VeiculoResponseDTO> buscarPorPlaca(@PathVariable String placa) {
        return ResponseEntity.ok(veiculoService.buscarPorPlaca(placa));
    }

    @GetMapping("/marca/{marca}")
    public List<VeiculoResponseDTO> buscarPorMarca(@PathVariable String marca) {
        return veiculoService.buscarPorMarca(marca);
    }

    @GetMapping("/modelo/{modelo}")
    public List<VeiculoResponseDTO> buscarPorModelo(@PathVariable String modelo) {
        return veiculoService.buscarPorModelo(modelo);
    }

    @GetMapping("/tipo/{tipo}")
    public List<VeiculoResponseDTO> buscarPorTipo(@PathVariable Tipo tipo) {
        return veiculoService.buscarPorTipo(tipo);
    }

    @PostMapping
    public ResponseEntity<VeiculoResponseDTO> salvar(@RequestBody @Valid VeiculoRequestDTO dto) {
        return ResponseEntity.ok(veiculoService.salvar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VeiculoResponseDTO> atualizar(@PathVariable Long id,
                                                        @RequestBody @Valid VeiculoRequestDTO dto) {
        return ResponseEntity.ok(veiculoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        veiculoService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== VÍNCULO COM MOTORISTA ====================

    @PutMapping("/{veiculoId}/motoristas/{motoristaId}")
    public ResponseEntity<VeiculoResponseDTO> vincularMotorista(
            @PathVariable Long veiculoId,
            @PathVariable Long motoristaId) {
        return ResponseEntity.ok(veiculoService.vincularMotorista(veiculoId, motoristaId));
    }

    @DeleteMapping("/{veiculoId}/motoristas/{motoristaId}")
    public ResponseEntity<VeiculoResponseDTO> desvincularMotorista(
            @PathVariable Long veiculoId,
            @PathVariable Long motoristaId) {
        return ResponseEntity.ok(veiculoService.desvincularMotorista(veiculoId, motoristaId));
    }

    @GetMapping("/{veiculoId}/motoristas")
    public ResponseEntity<List<MotoristaResponseDTO>> listarMotoristasDoVeiculo(@PathVariable Long veiculoId) {
        return ResponseEntity.ok(veiculoService.listarMotoristasDoVeiculo(veiculoId));
    }

    @GetMapping("/{veiculoId}/vinculos-motoristas")
    public List<VinculoMotoristaVeiculoResponseDTO> listarVinculosMotoristas(@PathVariable Long veiculoId) {
        return vinculoService.listarPorVeiculo(veiculoId);
    }
}
