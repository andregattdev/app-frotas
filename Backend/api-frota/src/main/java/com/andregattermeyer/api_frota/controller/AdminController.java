package com.andregattermeyer.api_frota.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

import com.andregattermeyer.api_frota.auth.dto.ContaMotoristaResponse;
import com.andregattermeyer.api_frota.auth.dto.CriarContaMotoristaRequest;
import com.andregattermeyer.api_frota.auth.dto.ContaAssistenteResponse;
import com.andregattermeyer.api_frota.auth.dto.CriarContaAssistenteRequest;
import com.andregattermeyer.api_frota.dto.response.VeiculoResponseDTO;
import com.andregattermeyer.api_frota.dto.response.MotoristaResponseDTO;
import com.andregattermeyer.api_frota.dto.response.RegistroParadaResponseDTO;
import com.andregattermeyer.api_frota.dto.request.RotaDiariaRequestDTO;
import com.andregattermeyer.api_frota.dto.request.AtualizarRotaDiariaRequestDTO;
import com.andregattermeyer.api_frota.dto.response.RotaDiariaResponseDTO;
import com.andregattermeyer.api_frota.service.AdminService;
import com.andregattermeyer.api_frota.service.RegistroParadaService;
import com.andregattermeyer.api_frota.service.RotaDiariaService;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final RegistroParadaService registroParadaService;
    private final RotaDiariaService rotaDiariaService;

    public AdminController(
            AdminService adminService,
            RegistroParadaService registroParadaService,
            RotaDiariaService rotaDiariaService) {
        this.adminService = adminService;
        this.registroParadaService = registroParadaService;
        this.rotaDiariaService = rotaDiariaService;
    }

    @GetMapping("/motoristas")
    public ResponseEntity<List<MotoristaResponseDTO>> listarMotoristasComConta() {
        return ResponseEntity.ok(adminService.listarMotoristasComConta());
    }

    @GetMapping("/registros-paradas")
    public ResponseEntity<List<RegistroParadaResponseDTO>> listarRegistrosParadas() {
        return ResponseEntity.ok(registroParadaService.listarTodos());
    }

    @GetMapping("/registros-paradas/{registroId}/foto")
    public ResponseEntity<Resource> obterFotoRegistroParada(@PathVariable Long registroId) {
        Resource foto = registroParadaService.obterFoto(registroId);
        MediaType tipo = foto.getFilename() != null && foto.getFilename().endsWith(".png")
                ? MediaType.IMAGE_PNG
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(tipo)
                .header("Cache-Control", "private, no-store")
                .body(foto);
    }

    @GetMapping("/rotas-diarias")
    public ResponseEntity<List<RotaDiariaResponseDTO>> listarRotasDiarias() {
        return ResponseEntity.ok(rotaDiariaService.listarTodas());
    }

    @PostMapping("/rotas-diarias")
    public ResponseEntity<RotaDiariaResponseDTO> criarRotaDiaria(
            @RequestBody @Valid RotaDiariaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rotaDiariaService.criar(request));
    }

    @PutMapping("/rotas-diarias/{rotaId}")
    public ResponseEntity<RotaDiariaResponseDTO> atualizarRotaDiaria(
            @PathVariable Long rotaId,
            @RequestBody @Valid AtualizarRotaDiariaRequestDTO request) {
        return ResponseEntity.ok(rotaDiariaService.atualizar(rotaId, request));
    }

    @GetMapping("/veiculos/{veiculoId}/registros-paradas")
    public ResponseEntity<List<RegistroParadaResponseDTO>> listarRegistrosParadasPorVeiculo(
            @PathVariable Long veiculoId) {
        return ResponseEntity.ok(registroParadaService.listarPorVeiculo(veiculoId));
    }

    @PostMapping("/motoristas/{motoristaId}/conta")
    public ResponseEntity<ContaMotoristaResponse> criarContaMotorista(
            @PathVariable Long motoristaId,
            @RequestBody @Valid CriarContaMotoristaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.criarContaMotorista(motoristaId, request));
    }

    @PostMapping("/assistentes")
    public ResponseEntity<ContaAssistenteResponse> criarContaAssistente(
            @RequestBody @Valid CriarContaAssistenteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.criarContaAssistente(request));
    }

    @PostMapping("/veiculos/{veiculoId}/motoristas/{motoristaId}")
    public ResponseEntity<VeiculoResponseDTO> atribuirMotorista(
            @PathVariable Long veiculoId,
            @PathVariable Long motoristaId) {
        return ResponseEntity.ok(adminService.atribuirMotorista(veiculoId, motoristaId));
    }
}
