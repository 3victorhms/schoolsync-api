package br.cefetmg.schoolsync_api.controller;

import br.cefetmg.schoolsync_api.security.UsuarioAtual;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import br.cefetmg.schoolsync_api.dto.notificacao.NotificacaoConfiguracaoDTO;
import br.cefetmg.schoolsync_api.dto.notificacao.DispositivoPushDTO;
import br.cefetmg.schoolsync_api.dto.notificacao.NotificacaoResponseDTO;
import br.cefetmg.schoolsync_api.service.NotificacaoService;
import br.cefetmg.schoolsync_api.service.NotificacaoPushService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notificacoes")
@Tag(name = "Notificacao")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoService notificacaoService;
    private final NotificacaoPushService notificacaoPushService;
    private final UsuarioAtual usuarioAtual;

    @PostMapping("/push/dispositivo")
    public ResponseEntity<Void> registrarDispositivo(@Valid @RequestBody DispositivoPushDTO dto) {
        notificacaoPushService.registrarDispositivo(dto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/push/dispositivo")
    public ResponseEntity<Void> removerDispositivo(@RequestParam String token) {
        notificacaoPushService.removerDispositivo(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/push/teste")
    public ResponseEntity<Void> testarPush() {
        notificacaoPushService.testarEnvioParaUsuarioAutenticado();
        return ResponseEntity.noContent().build();
    }

    /** Notificações do usuário logado. */
    @GetMapping
    public ResponseEntity<List<NotificacaoResponseDTO>> listarMinhas() {
        return ResponseEntity.ok(notificacaoService.listarPorUsuario(usuarioAtual.id()));
    }

    @PutMapping("/{id}/lida")
    public ResponseEntity<Void> marcarComoLida(@PathVariable String id) {
        notificacaoService.validarDono(id, usuarioAtual.id());
        notificacaoService.marcarComoLida(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/lidas")
    public ResponseEntity<Void> marcarTodasComoLidas() {
        notificacaoService.marcarTodasComoLidas(usuarioAtual.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/configuracoes")
    public ResponseEntity<NotificacaoConfiguracaoDTO> buscarConfiguracao() {
        return ResponseEntity.ok(notificacaoService.buscarConfiguracao(usuarioAtual.id()));
    }

    @PutMapping("/configuracoes")
    public ResponseEntity<NotificacaoConfiguracaoDTO> salvarConfiguracao(
            @Valid @RequestBody NotificacaoConfiguracaoDTO dto
    ) {
        return ResponseEntity.ok(notificacaoService.salvarConfiguracao(usuarioAtual.id(), dto));
    }

    /** O EventSource não manda cabeçalho: o token vem em ?token= e o filtro JWT autentica por ele. */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return notificacaoService.conectar(usuarioAtual.id());
    }
}
