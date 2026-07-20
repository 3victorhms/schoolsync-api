package br.cefetmg.schoolsync_api.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import br.cefetmg.schoolsync_api.dto.notificacao.NotificacaoConfiguracaoDTO;
import br.cefetmg.schoolsync_api.dto.notificacao.NotificacaoResponseDTO;
import br.cefetmg.schoolsync_api.entity.Notificacao;
import br.cefetmg.schoolsync_api.entity.NotificacaoConfiguracao;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.NotificacaoConfiguracaoRepository;
import br.cefetmg.schoolsync_api.repository.NotificacaoRepository;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private static final Long SSE_TIMEOUT = 30L * 60L * 1000L;

    private final NotificacaoRepository notificacaoRepository;
    private final NotificacaoConfiguracaoRepository configuracaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final Map<String, List<SseEmitter>> emittersPorUsuario = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public List<NotificacaoResponseDTO> listarPorUsuario(String idUsuario) {
        validarUsuario(idUsuario);

        return notificacaoRepository.findByUsuario_IdOrderByHorarioDesc(idUsuario)
                .stream()
                .map(NotificacaoResponseDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void marcarComoLida(String idNotificacao) {
        Notificacao notificacao = notificacaoRepository.findById(idNotificacao)
                .orElseThrow(() -> new EntityNotFoundException("Notificacao nao encontrada"));

        notificacao.setLido(true);
        notificacaoRepository.save(notificacao);
    }

    @Transactional
    public void marcarTodasComoLidas(String idUsuario) {
        validarUsuario(idUsuario);

        List<Notificacao> notificacoes = notificacaoRepository.findByUsuario_IdAndLidoFalse(idUsuario);
        notificacoes.forEach(notificacao -> notificacao.setLido(true));
        notificacaoRepository.saveAll(notificacoes);
    }

    @Transactional
    public NotificacaoConfiguracaoDTO buscarConfiguracao(String idUsuario) {
        NotificacaoConfiguracao configuracao = buscarOuCriarConfiguracao(idUsuario);
        return new NotificacaoConfiguracaoDTO(configuracao);
    }

    @Transactional
    public NotificacaoConfiguracaoDTO salvarConfiguracao(String idUsuario, NotificacaoConfiguracaoDTO dto) {
        NotificacaoConfiguracao configuracao = buscarOuCriarConfiguracao(idUsuario);

        configuracao.setNoAplicativo(dto.isNoAplicativo());
        configuracao.setPush(dto.isPush());
        configuracao.setLembreteDias(dto.getLembreteDias());

        return new NotificacaoConfiguracaoDTO(configuracaoRepository.save(configuracao));
    }

    public SseEmitter conectar(String idUsuario) {
        validarUsuario(idUsuario);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emittersPorUsuario.computeIfAbsent(idUsuario, key -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removerEmitter(idUsuario, emitter));
        emitter.onTimeout(() -> removerEmitter(idUsuario, emitter));
        emitter.onError(error -> removerEmitter(idUsuario, emitter));

        return emitter;
    }

    @Transactional
    public NotificacaoResponseDTO criarParaUsuario(
            String idUsuario,
            String tipo,
            String titulo,
            String mensagem,
            String targetId
    ) {
        NotificacaoConfiguracao configuracao = buscarOuCriarConfiguracao(idUsuario);

        if (!configuracao.isNoAplicativo()) {
            return null;
        }

        Notificacao notificacao = new Notificacao();
        notificacao.setUsuario(configuracao.getUsuario());
        notificacao.setTipo(tipo);
        notificacao.setTitulo(titulo);
        notificacao.setMensagem(mensagem);
        notificacao.setTargetId(targetId);
        notificacao.setHorario(LocalDateTime.now());
        notificacao.setLido(false);

        NotificacaoResponseDTO response = new NotificacaoResponseDTO(notificacaoRepository.save(notificacao));
        enviar(idUsuario, response);

        return response;
    }

    private NotificacaoConfiguracao buscarOuCriarConfiguracao(String idUsuario) {
        return configuracaoRepository.findByUsuario_Id(idUsuario)
                .orElseGet(() -> {
                    Usuario usuario = usuarioRepository.findById(idUsuario)
                            .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));

                    NotificacaoConfiguracao configuracao = new NotificacaoConfiguracao();
                    configuracao.setUsuario(usuario);
                    configuracao.setNoAplicativo(true);
                    configuracao.setPush(false);
                    configuracao.setLembreteDias(3);

                    return configuracaoRepository.save(configuracao);
                });
    }

    private void validarUsuario(String idUsuario) {
        if (!usuarioRepository.existsById(idUsuario)) {
            throw new EntityNotFoundException("Usuario nao encontrado");
        }
    }

    private void enviar(String idUsuario, NotificacaoResponseDTO notificacao) {
        List<SseEmitter> emitters = emittersPorUsuario.get(idUsuario);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(notificacao.getTipo())
                        .data(notificacao));
            } catch (IOException ex) {
                removerEmitter(idUsuario, emitter);
            }
        }
    }

    private void removerEmitter(String idUsuario, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersPorUsuario.get(idUsuario);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }
}
