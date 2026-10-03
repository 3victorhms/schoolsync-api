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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.cefetmg.schoolsync_api.dto.atividade.AtividadeRequestDTO;
import br.cefetmg.schoolsync_api.dto.atividade.AtividadeResponseDTO;
import br.cefetmg.schoolsync_api.security.UsuarioAtual;
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
    public ResponseEntity<AtividadeResponseDTO> criar(@Valid @RequestBody AtividadeRequestDTO dto) {
        String idUsuario = usuarioAtual.id();
        atividadeService.validarMembroDaSala(dto.getIdSala(), idUsuario);
        AtividadeResponseDTO atividade = atividadeService.criar(dto, idUsuario);
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
    public ResponseEntity<AtividadeResponseDTO> buscarPorId(@PathVariable String id) {
        String idUsuario = usuarioAtual.id();
        atividadeService.validarAcessoAtividade(id, idUsuario);
        return ResponseEntity.ok(atividadeService.buscarPorId(id, idUsuario));
    }

    @GetMapping("/sala/{idSala}")
    public ResponseEntity<List<AtividadeResponseDTO>> listarPorSala(@PathVariable String idSala) {
        atividadeService.validarMembroDaSala(idSala, usuarioAtual.id());
        return ResponseEntity.ok(atividadeService.listarPorSala(idSala));
    }

    @GetMapping("/caderno")
    public ResponseEntity<List<AtividadeResponseDTO>> listarMeuCaderno() {
        return ResponseEntity.ok(atividadeService.listarPorUsuarioNoCaderno(usuarioAtual.id()));
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
    public ResponseEntity<Void> adicionarNoCaderno(@PathVariable String idAtividade) {
        String idUsuario = usuarioAtual.id();
        atividadeService.validarAcessoAtividade(idAtividade, idUsuario);
        atividadeService.adicionarNoCaderno(idAtividade, idUsuario);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{idAtividade}/caderno")
    public ResponseEntity<Void> removerDoCaderno(@PathVariable String idAtividade) {
        atividadeService.removerDoCaderno(idAtividade, usuarioAtual.id());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{idAtividade}/status")
    public ResponseEntity<Void> alterarStatus(
            @PathVariable String idAtividade,
            @RequestParam String status
    ) {
        atividadeService.alterarStatus(idAtividade, usuarioAtual.id(), status);
        return ResponseEntity.noContent().build();
    }
}
