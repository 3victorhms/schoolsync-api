package br.cefetmg.schoolsync_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.cefetmg.schoolsync_api.dto.sistema.ManutencaoDTO;
import br.cefetmg.schoolsync_api.service.ManutencaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/sistema")
@Tag(name = "Sistema")
@RequiredArgsConstructor
public class SistemaController {

    private final ManutencaoService manutencaoService;

    // público: a tela de manutenção do app consulta aqui pra saber quando a manutenção acabou
    @GetMapping("/status")
    @Operation(summary = "Saber se o sistema está em manutenção")
    public ResponseEntity<ManutencaoDTO> status() {
        return ResponseEntity.ok(manutencaoService.estado().publico());
    }
}
