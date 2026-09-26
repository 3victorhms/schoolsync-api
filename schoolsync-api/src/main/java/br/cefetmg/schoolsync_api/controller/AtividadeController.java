package br.cefetmg.schoolsync_api.controller;

import br.cefetmg.schoolsync_api.security.UsuarioAtual;
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
@Tag(name = "Atividade")
@RequiredArgsConstructor
public class AtividadeController {

    private final AtividadeService atividadeService;
    private final UsuarioAtual usuarioAtual;

    @PostMapping
    public ResponseEntity<AtividadeResponseDTO> criar(
            @Valid @RequestBody AtividadeRequestDTO dto
    ) {
        usuarioAtual.validar(dto.getIdCriador());
        atividadeService.validarMembroDaSala(dto.getIdSala(), usuarioAtual.id());
        AtividadeResponseDTO atividade = atividadeService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(atividade);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AtividadeResponseDTO> atualizar(
            @PathVariable String id,
            @Valid @RequestBody AtividadeRequestDTO dto
    ) {
        atividadeService.validarPodeGerenciar(id, usuarioAtual.id());
        return ResponseEntity.ok(atividadeService.atualizar(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AtividadeResponseDTO> buscarPorId(
            @PathVariable String id,
            @RequestParam String idUsuarioLogado
    ) {
        usuarioAtual.validar(idUsuarioLogado);
        atividadeService.validarAcessoAtividade(id, idUsuarioLogado);
        return ResponseEntity.ok(
                atividadeService.buscarPorId(id, idUsuarioLogado)
        );
    }

    @GetMapping("/sala/{idSala}")
    public ResponseEntity<List<AtividadeResponseDTO>> listarPorSala(@PathVariable String idSala) {
        atividadeService.validarMembroDaSala(idSala, usuarioAtual.id());
        return ResponseEntity.ok(atividadeService.listarPorSala(idSala));
    }

    @GetMapping("/caderno/usuario/{idUsuario}")
    public ResponseEntity<List<AtividadeResponseDTO>> listarPorUsuarioNoCaderno(@PathVariable String idUsuario) {
        usuarioAtual.validar(idUsuario);
        return ResponseEntity.ok(atividadeService.listarPorUsuarioNoCaderno(idUsuario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        atividadeService.validarPodeGerenciar(id, usuarioAtual.id());
        atividadeService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sala/{idSala}")
    public ResponseEntity<Void> excluirAtividadesDaSala(@PathVariable String idSala) {
        atividadeService.validarLiderDaSala(idSala, usuarioAtual.id());
        atividadeService.excluirAtividadesDaSala(idSala);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{idAtividade}/caderno")
    public ResponseEntity<Void> adicionarNoCaderno(
            @PathVariable String idAtividade,
            @RequestParam String idUsuario
    ) {
        usuarioAtual.validar(idUsuario);
        atividadeService.validarAcessoAtividade(idAtividade, idUsuario);
        atividadeService.adicionarNoCaderno(idAtividade, idUsuario);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{idAtividade}/caderno")
    public ResponseEntity<Void> removerDoCaderno(
            @PathVariable String idAtividade,
            @RequestParam String idUsuario
    ) {
        usuarioAtual.validar(idUsuario);
        atividadeService.removerDoCaderno(idAtividade, idUsuario);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{idAtividade}/status")
    public ResponseEntity<Void> alterarStatus(
            @PathVariable String idAtividade,
            @RequestParam String idUsuario,
            @RequestParam String status
    ) {
        usuarioAtual.validar(idUsuario);
        atividadeService.alterarStatus(idAtividade, idUsuario, status);
        return ResponseEntity.noContent().build();
    }
}
