package br.cefetmg.schoolsync_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.cefetmg.schoolsync_api.dto.atividade.AtividadeRequestDTO;
import br.cefetmg.schoolsync_api.dto.atividade.AtividadeResponseDTO;
import br.cefetmg.schoolsync_api.service.AtividadeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/atividades")
@CrossOrigin(origins = "http://localhost:8100")
@Tag(name = "Atividade")
@RequiredArgsConstructor
public class AtividadeController {

    private final AtividadeService atividadeService;

    @PostMapping
    public ResponseEntity<AtividadeResponseDTO> criar(
            @Valid @RequestBody AtividadeRequestDTO dto
    ) {
        AtividadeResponseDTO atividade = atividadeService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(atividade);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AtividadeResponseDTO> atualizar(
            @PathVariable String id,
            @Valid @RequestBody AtividadeRequestDTO dto
    ) {
        return ResponseEntity.ok(atividadeService.atualizar(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AtividadeResponseDTO> buscarPorId(
            @PathVariable String id,
            @RequestParam String idUsuarioLogado
    ) {
        return ResponseEntity.ok(
                atividadeService.buscarPorId(id, idUsuarioLogado)
        );
    }

    @GetMapping("/sala/{idSala}")
    public ResponseEntity<List<AtividadeResponseDTO>> listarPorSala(@PathVariable String idSala) {
        return ResponseEntity.ok(atividadeService.listarPorSala(idSala));
    }

    @GetMapping("/caderno/usuario/{idUsuario}")
    public ResponseEntity<List<AtividadeResponseDTO>> listarPorUsuarioNoCaderno(@PathVariable String idUsuario) {
        return ResponseEntity.ok(atividadeService.listarPorUsuarioNoCaderno(idUsuario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        atividadeService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sala/{idSala}")
    public ResponseEntity<Void> excluirAtividadesDaSala(@PathVariable String idSala) {
        atividadeService.excluirAtividadesDaSala(idSala);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{idAtividade}/caderno")
    public ResponseEntity<Void> adicionarNoCaderno(
            @PathVariable String idAtividade,
            @RequestParam String idUsuario
    ) {
        atividadeService.adicionarNoCaderno(idAtividade, idUsuario);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{idAtividade}/caderno")
    public ResponseEntity<Void> removerDoCaderno(
            @PathVariable String idAtividade,
            @RequestParam String idUsuario
    ) {
        atividadeService.removerDoCaderno(idAtividade, idUsuario);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{idAtividade}/status")
    public ResponseEntity<Void> alterarStatus(
            @PathVariable String idAtividade,
            @RequestParam String idUsuario,
            @RequestParam String status
    ) {
        atividadeService.alterarStatus(idAtividade, idUsuario, status);
        return ResponseEntity.noContent().build();
    }
}
