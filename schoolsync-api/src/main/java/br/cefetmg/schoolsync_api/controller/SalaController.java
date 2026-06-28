package br.cefetmg.schoolsync_api.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.cefetmg.schoolsync_api.dto.sala.SalaRequestDTO;
import br.cefetmg.schoolsync_api.dto.sala.SalaResponseDTO;
import br.cefetmg.schoolsync_api.dto.sala.SalaResumoDTO;
import br.cefetmg.schoolsync_api.service.SalaService;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/salas")
@CrossOrigin(origins = "http://localhost:8100")
@Tag(name = "Sala")
public class SalaController {

    @Autowired
    private SalaService salaService;

    @PostMapping
    @Operation(summary = "Criar sala")
    public ResponseEntity<SalaResponseDTO> criar(
            @Valid @RequestBody SalaRequestDTO salaRequestDTO,
            @RequestParam String idLider
    ) {
        SalaResponseDTO sala = salaService.criar(salaRequestDTO, idLider);
        return ResponseEntity.status(HttpStatus.CREATED).body(sala);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar sala")
    public ResponseEntity<SalaResponseDTO> atualizar(
            @PathVariable String id,
            @Valid @RequestBody SalaRequestDTO salaRequestDTO
    ) {
        return ResponseEntity.ok(salaService.atualizar(id, salaRequestDTO));
    }

    @PostMapping("/entrar")
    @Operation(summary = "Entrar em uma sala via código de convite")
    public ResponseEntity<SalaResponseDTO> entrar(
            @RequestParam String codigoConvite,
            @RequestParam String idUsuario
    ) {
        return ResponseEntity.ok(salaService.entrar(codigoConvite, idUsuario));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar sala por ID")
    public ResponseEntity<SalaResponseDTO> buscarPorId(
            @PathVariable String id,
            @RequestParam String idUsuarioLogado
    ) {
        return ResponseEntity.ok(salaService.buscarPorId(id, idUsuarioLogado));
    }

    @GetMapping("/usuario/{idUsuario}")
    @Operation(summary = "Listar salas que o usuário participa")
    public ResponseEntity<List<SalaResumoDTO>> listarPorUsuario(@PathVariable String idUsuario) {
        return ResponseEntity.ok(salaService.listarPorUsuario(idUsuario));
    }

    @DeleteMapping("/{idSala}/membros/{idUsuarioRemover}")
    @Operation(summary = "Remover membro da sala")
    public ResponseEntity<Void> excluirMembro(
            @PathVariable String idSala,
            @PathVariable String idUsuarioRemover,
            @RequestParam String idUsuarioLogado
    ) {
        salaService.excluirMembro(idSala, idUsuarioRemover, idUsuarioLogado);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{idSala}/sair")
    @Operation(summary = "Sair da sala")
    public ResponseEntity<Void> sair(
            @PathVariable String idSala,
            @RequestParam String idUsuario
    ) {
        salaService.sair(idSala, idUsuario);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir sala")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        salaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
