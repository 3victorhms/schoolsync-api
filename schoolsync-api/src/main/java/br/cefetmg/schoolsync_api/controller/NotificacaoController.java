package br.cefetmg.schoolsync_api.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import br.cefetmg.schoolsync_api.dto.notificacao.NotificacaoConfiguracaoDTO;
import br.cefetmg.schoolsync_api.dto.notificacao.NotificacaoResponseDTO;
import br.cefetmg.schoolsync_api.service.NotificacaoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notificacoes")
@CrossOrigin(origins = "http://localhost:8100")
@Tag(name = "Notificacao")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<NotificacaoResponseDTO>> listarPorUsuario(@PathVariable String idUsuario) {
        return ResponseEntity.ok(notificacaoService.listarPorUsuario(idUsuario));
    }

    @PutMapping("/{id}/lida")
    public ResponseEntity<Void> marcarComoLida(@PathVariable String id) {
        notificacaoService.marcarComoLida(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/usuario/{idUsuario}/lidas")
    public ResponseEntity<Void> marcarTodasComoLidas(@PathVariable String idUsuario) {
        notificacaoService.marcarTodasComoLidas(idUsuario);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/usuario/{idUsuario}/configuracoes")
    public ResponseEntity<NotificacaoConfiguracaoDTO> buscarConfiguracao(@PathVariable String idUsuario) {
        return ResponseEntity.ok(notificacaoService.buscarConfiguracao(idUsuario));
    }

    @PutMapping("/usuario/{idUsuario}/configuracoes")
    public ResponseEntity<NotificacaoConfiguracaoDTO> salvarConfiguracao(
            @PathVariable String idUsuario,
            @Valid @RequestBody NotificacaoConfiguracaoDTO dto
    ) {
        return ResponseEntity.ok(notificacaoService.salvarConfiguracao(idUsuario, dto));
    }

    @GetMapping(value = "/usuario/{idUsuario}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String idUsuario) {
        return notificacaoService.conectar(idUsuario);
    }
}
