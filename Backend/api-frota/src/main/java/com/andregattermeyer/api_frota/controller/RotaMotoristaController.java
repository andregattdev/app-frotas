package com.andregattermeyer.api_frota.controller;

import java.util.List;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import com.andregattermeyer.api_frota.dto.request.RegistroParadaRequestDTO;
import com.andregattermeyer.api_frota.dto.response.RegistroParadaResponseDTO;
import com.andregattermeyer.api_frota.dto.response.RotaMotoristaResponseDTO;
import com.andregattermeyer.api_frota.service.RegistroParadaService;
import com.andregattermeyer.api_frota.service.RotaDiariaService;
import com.andregattermeyer.api_frota.service.RotaMotoristaService;

@RestController
@RequestMapping("/api/rotas")
public class RotaMotoristaController {

    private final RotaMotoristaService rotaMotoristaService;
    private final RotaDiariaService rotaDiariaService;
    private final RegistroParadaService registroParadaService;

    public RotaMotoristaController(
            RotaMotoristaService rotaMotoristaService,
            RotaDiariaService rotaDiariaService,
            RegistroParadaService registroParadaService) {
        this.rotaMotoristaService = rotaMotoristaService;
        this.rotaDiariaService = rotaDiariaService;
        this.registroParadaService = registroParadaService;
    }

    @GetMapping("/minhas")
    public List<RotaMotoristaResponseDTO> minhasRotas(@AuthenticationPrincipal UserDetails usuario) {
        List<RotaMotoristaResponseDTO> rotasDiarias = rotaDiariaService.listarDoMotorista(usuario.getUsername());
        List<RotaMotoristaResponseDTO> rotasLegadas = rotaMotoristaService.listarRotasDoMotorista(usuario.getUsername());
        Set<Long> veiculosComRotaHoje = rotasDiarias.stream()
                .filter(rota -> LocalDate.now().equals(rota.dataRota()))
                .map(rota -> rota.veiculo().getId())
                .collect(Collectors.toSet());
        var rotas = new ArrayList<>(rotasDiarias);
        rotasLegadas.stream()
                .filter(rota -> !veiculosComRotaHoje.contains(rota.veiculo().getId()))
                .forEach(rotas::add);
        return rotas;
    }

    @PostMapping("/diarias/{rotaId}/finalizar")
    public RotaMotoristaResponseDTO finalizarDia(
            @PathVariable Long rotaId,
            @AuthenticationPrincipal UserDetails usuario) {
        return rotaDiariaService.finalizar(rotaId, usuario.getUsername());
    }

    @PostMapping("/veiculos/{veiculoId}/finalizar-dia")
    public RotaMotoristaResponseDTO finalizarDiaDaRotaAtual(
            @PathVariable Long veiculoId,
            @AuthenticationPrincipal UserDetails usuario) {
        return rotaDiariaService.finalizarRotaAtual(veiculoId, usuario.getUsername());
    }

    @PostMapping("/paradas/{paradaId}/registros")
    public ResponseEntity<RegistroParadaResponseDTO> registrarParada(
            @PathVariable Long paradaId,
            @RequestPart("request") @Valid RegistroParadaRequestDTO request,
            @RequestPart("foto") MultipartFile foto,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @AuthenticationPrincipal UserDetails usuario) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registroParadaService.registrar(paradaId, usuario.getUsername(), request, foto, latitude, longitude));
    }
}
