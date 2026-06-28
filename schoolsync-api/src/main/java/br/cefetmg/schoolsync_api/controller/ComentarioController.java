package br.cefetmg.schoolsync_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.cefetmg.schoolsync_api.dto.comentario.ComentarioRequestDTO;
import br.cefetmg.schoolsync_api.dto.comentario.ComentarioResponseDTO;
import br.cefetmg.schoolsync_api.dto.comentario.ComentarioUpdateDTO;
import br.cefetmg.schoolsync_api.service.ComentarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@CrossOrigin(origins = "http://localhost:8100")
@Tag(name = "Comentario")
@RequiredArgsConstructor
public class ComentarioController {

    private final ComentarioService comentarioService;

    @GetMapping("/atividades/{idAtividade}/comentarios")
    public ResponseEntity<List<ComentarioResponseDTO>> listarPorAtividade(
            @PathVariable String idAtividade
    ) {
        return ResponseEntity.ok(comentarioService.listarPorAtividade(idAtividade));
    }

    @PostMapping("/atividades/{idAtividade}/comentarios")
    public ResponseEntity<ComentarioResponseDTO> criar(
            @PathVariable String idAtividade,
            @Valid @RequestBody ComentarioRequestDTO dto
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(comentarioService.criar(idAtividade, dto));
    }

    @PutMapping("/comentarios/{idComentario}")
    public ResponseEntity<ComentarioResponseDTO> atualizar(
            @PathVariable String idComentario,
            @Valid @RequestBody ComentarioUpdateDTO dto
    ) {
        return ResponseEntity.ok(comentarioService.atualizar(idComentario, dto));
    }

    @DeleteMapping("/comentarios/{idComentario}")
    public ResponseEntity<Void> excluir(
            @PathVariable String idComentario,
            @RequestParam String idUsuario
    ) {
        comentarioService.excluir(idComentario, idUsuario);
        return ResponseEntity.noContent().build();
    }
}
