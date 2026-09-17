package br.cefetmg.schoolsync_api.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;

import br.cefetmg.schoolsync_api.dto.notificacao.DispositivoPushDTO;
import br.cefetmg.schoolsync_api.entity.DispositivoPush;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.DispositivoPushRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificacaoPushService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoPushService.class);
    private static final String CANAL_ANDROID = "schoolsync_notifications";

    private final DispositivoPushRepository dispositivoRepository;
    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;

    @Transactional
    public void registrarDispositivo(DispositivoPushDTO dto) {
        Usuario usuario = usuarioAutenticado();
        DispositivoPush dispositivo = dispositivoRepository.findByToken(dto.getToken())
                .orElseGet(DispositivoPush::new);

        // Um token pertence ao usuário atualmente conectado naquele aparelho.
        // Se outra conta entrar no mesmo celular, o vínculo anterior é substituído.
        dispositivo.setUsuario(usuario);
        dispositivo.setToken(dto.getToken());
        dispositivo.setPlataforma(dto.getPlataforma());
        dispositivoRepository.save(dispositivo);
    }

    @Transactional
    public void removerDispositivo(String token) {
        Usuario usuario = usuarioAutenticado();
        dispositivoRepository.deleteByUsuario_IdAndToken(usuario.getId(), token);
    }

    @Async
    public void enviar(String idUsuario, String tipo, String titulo, String mensagem, String targetId) {
        try {
            enviarAgora(idUsuario, tipo, titulo, mensagem, targetId, false);
        } catch (FirebaseMessagingException ex) {
            log.warn("Não foi possível enviar notificação push ao usuário {}: {}", idUsuario, ex.getMessage());
        }
    }

    /** Dispara uma notificação real para o dispositivo do usuário autenticado. */
    public void testarEnvioParaUsuarioAutenticado() {
        Usuario usuario = usuarioAutenticado();
        try {
            int enviados = enviarAgora(
                    usuario.getId(),
                    "TESTE_NOTIFICACAO",
                    "Teste do SchoolSync",
                    "Esta é uma notificação enviada pelo servidor.",
                    null,
                    true);

            if (enviados == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "O Firebase não confirmou o envio da notificação");
            }
        } catch (FirebaseMessagingException ex) {
            log.warn("Falha no teste de push para o usuário {}: {}", usuario.getId(), ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "O Firebase recusou a notificação de teste", ex);
        }
    }

    private int enviarAgora(
            String idUsuario,
            String tipo,
            String titulo,
            String mensagem,
            String targetId,
            boolean exigido
    ) throws FirebaseMessagingException {
        FirebaseMessaging firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
        if (firebaseMessaging == null) {
            if (exigido) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "O Firebase não está configurado no servidor");
            }
            log.debug("Push ignorado: Firebase não está habilitado no servidor");
            return 0;
        }

        List<DispositivoPush> dispositivos = dispositivoRepository.findAllByUsuario_Id(idUsuario);
        if (dispositivos.isEmpty()) {
            if (exigido) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Este celular ainda não está registrado para receber notificações push");
            }
            return 0;
        }

        List<Message> mensagens = dispositivos.stream()
                .map(dispositivo -> montarMensagem(dispositivo.getToken(), tipo, titulo, mensagem, targetId))
                .toList();
        BatchResponse resposta = firebaseMessaging.sendEach(mensagens);
        removerTokensInvalidos(dispositivos, resposta.getResponses());
        return resposta.getSuccessCount();
    }

    private Message montarMensagem(String token, String tipo, String titulo, String mensagem, String targetId) {
        String tag = targetId == null || targetId.isBlank() ? tipo : tipo + ":" + targetId;
        Message.Builder builder = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder().setTitle(titulo).setBody(mensagem).build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setChannelId(CANAL_ANDROID)
                                .setTag(tag)
                                .build())
                        .build())
                .putData("tipo", tipo == null ? "" : tipo);

        if (targetId != null) builder.putData("targetId", targetId);
        return builder.build();
    }

    private void removerTokensInvalidos(List<DispositivoPush> dispositivos, List<SendResponse> respostas) {
        List<DispositivoPush> invalidos = new ArrayList<>();
        for (int i = 0; i < respostas.size(); i++) {
            SendResponse resposta = respostas.get(i);
            if (resposta.isSuccessful() || resposta.getException() == null) continue;

            MessagingErrorCode codigo = resposta.getException().getMessagingErrorCode();
            if (codigo == MessagingErrorCode.UNREGISTERED || codigo == MessagingErrorCode.INVALID_ARGUMENT) {
                invalidos.add(dispositivos.get(i));
            }
        }
        if (!invalidos.isEmpty()) dispositivoRepository.deleteAll(invalidos);
    }

    private Usuario usuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (principal instanceof Usuario usuario) return usuario;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
    }
}
