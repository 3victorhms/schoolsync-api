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
import org.springframework.web.bind.annotation.RestController;

import br.cefetmg.schoolsync_api.dto.comentario.ComentarioRequestDTO;
import br.cefetmg.schoolsync_api.dto.comentario.ComentarioResponseDTO;
import br.cefetmg.schoolsync_api.dto.comentario.ComentarioUpdateDTO;
import br.cefetmg.schoolsync_api.security.UsuarioAtual;
import br.cefetmg.schoolsync_api.service.AtividadeService;
import br.cefetmg.schoolsync_api.service.ComentarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@Tag(name = "Comentario")
@RequiredArgsConstructor
public class ComentarioController {

    private final ComentarioService comentarioService;
    private final AtividadeService atividadeService;
    private final UsuarioAtual usuarioAtual;

    @GetMapping("/atividades/{idAtividade}/comentarios")
    public ResponseEntity<List<ComentarioResponseDTO>> listarPorAtividade(@PathVariable String idAtividade) {
        atividadeService.validarAcessoAtividade(idAtividade, usuarioAtual.id());
        return ResponseEntity.ok(comentarioService.listarPorAtividade(idAtividade));
    }

    @PostMapping("/atividades/{idAtividade}/comentarios")
    public ResponseEntity<ComentarioResponseDTO> criar(
            @PathVariable String idAtividade,
            @Valid @RequestBody ComentarioRequestDTO dto
    ) {
        String idUsuario = usuarioAtual.id();
        atividadeService.validarAcessoAtividade(idAtividade, idUsuario);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(comentarioService.criar(idAtividade, dto, idUsuario));
    }

    @PutMapping("/comentarios/{idComentario}")
    public ResponseEntity<ComentarioResponseDTO> atualizar(
            @PathVariable String idComentario,
            @Valid @RequestBody ComentarioUpdateDTO dto
    ) {
        return ResponseEntity.ok(comentarioService.atualizar(idComentario, dto, usuarioAtual.id()));
    }

    @DeleteMapping("/comentarios/{idComentario}")
    public ResponseEntity<Void> excluir(@PathVariable String idComentario) {
        comentarioService.excluir(idComentario, usuarioAtual.id());
        return ResponseEntity.noContent().build();
    }
}
