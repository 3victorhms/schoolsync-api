package br.cefetmg.schoolsync_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.cefetmg.schoolsync_api.dto.nota.NotaRequestDTO;
import br.cefetmg.schoolsync_api.dto.nota.NotaResponseDTO;
import br.cefetmg.schoolsync_api.security.UsuarioAtual;
import br.cefetmg.schoolsync_api.service.NotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Boletim pessoal: cada aluno só enxerga e altera as próprias notas. */
@RestController
@Tag(name = "Boletim")
@RequiredArgsConstructor
public class NotaController {

    private final NotaService notaService;
    private final UsuarioAtual usuarioAtual;

    @GetMapping("/salas/{idSala}/notas")
    @Operation(summary = "Minhas notas nesta sala")
    public ResponseEntity<List<NotaResponseDTO>> listarMinhas(@PathVariable String idSala) {
        return ResponseEntity.ok(notaService.listarMinhasDaSala(idSala, usuarioAtual.id()));
    }

    @PostMapping("/salas/{idSala}/notas")
    @Operation(summary = "Lançar nota (de uma atividade ou avulsa)")
    public ResponseEntity<NotaResponseDTO> lancar(
            @PathVariable String idSala,
            @Valid @RequestBody NotaRequestDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notaService.lancar(idSala, dto, usuarioAtual.id()));
    }

    @PutMapping("/notas/{idNota}")
    @Operation(summary = "Editar uma nota minha")
    public ResponseEntity<NotaResponseDTO> atualizar(
            @PathVariable String idNota,
            @Valid @RequestBody NotaRequestDTO dto
    ) {
        return ResponseEntity.ok(notaService.atualizar(idNota, dto, usuarioAtual.id()));
    }

    @DeleteMapping("/notas/{idNota}")
    @Operation(summary = "Excluir uma nota minha")
    public ResponseEntity<Void> excluir(@PathVariable String idNota) {
        notaService.excluir(idNota, usuarioAtual.id());
        return ResponseEntity.noContent().build();
    }
}
