package com.andregattermeyer.api_frota.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.andregattermeyer.api_frota.dto.request.ChecklistRequestDTO;
import com.andregattermeyer.api_frota.dto.response.ChecklistResponseDTO;
import com.andregattermeyer.api_frota.service.ChecklistService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/checklists")
public class ChecklistController {

    private final ChecklistService checklistService;

    public ChecklistController(ChecklistService checklistService) {
        this.checklistService = checklistService;
    }

    @GetMapping
    public List<ChecklistResponseDTO> listarTodos(@RequestParam(required = false) String busca) {
        return checklistService.listarTodos(busca);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChecklistResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(checklistService.buscarPorId(id));
    }

    @GetMapping("/veiculo/{veiculoId}")
    public List<ChecklistResponseDTO> listarPorVeiculo(@PathVariable Long veiculoId,
                                                        @RequestParam(required = false) String busca,
                                                        Authentication authentication) {
        if (ehMotorista(authentication)) {
            return checklistService.listarPorVeiculoDoMotorista(veiculoId, busca, authentication.getName());
        }
        return checklistService.listarPorVeiculo(veiculoId, busca);
    }

    @GetMapping("/motorista/{motoristaId}")
    public List<ChecklistResponseDTO> listarPorMotorista(@PathVariable Long motoristaId) {
        return checklistService.listarPorMotorista(motoristaId);
    }

    @GetMapping("/veiculo/{veiculoId}/ultimo")
    public ResponseEntity<ChecklistResponseDTO> buscarUltimoPorVeiculo(@PathVariable Long veiculoId) {
        return ResponseEntity.ok(checklistService.buscarUltimoPorVeiculo(veiculoId));
    }

    @PostMapping
    public ResponseEntity<ChecklistResponseDTO> salvar(@RequestBody @Valid ChecklistRequestDTO dto,
                                                        Authentication authentication) {
        if (ehMotorista(authentication)) {
            return ResponseEntity.ok(checklistService.salvarComoMotorista(dto, authentication.getName()));
        }
        if (ehAssistente(authentication)) {
            dto.setMotoristaId(null);
        }
        return ResponseEntity.ok(checklistService.salvar(dto));
    }

    private boolean ehAssistente(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ASSISTENTE_FROTA"));
    }

    private boolean ehMotorista(Authentication authentication) {
        boolean administrador = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_SUPER_ADMIN"));
        return !administrador && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_MOTORISTA"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChecklistResponseDTO> atualizar(@PathVariable Long id,
                                                          @RequestBody @Valid ChecklistRequestDTO dto) {
        return ResponseEntity.ok(checklistService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        checklistService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
