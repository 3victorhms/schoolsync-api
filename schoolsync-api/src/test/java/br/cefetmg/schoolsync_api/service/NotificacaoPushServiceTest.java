package br.cefetmg.schoolsync_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.firebase.messaging.FirebaseMessaging;

import br.cefetmg.schoolsync_api.dto.notificacao.DispositivoPushDTO;
import br.cefetmg.schoolsync_api.entity.DispositivoPush;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.DispositivoPushRepository;

@ExtendWith(MockitoExtension.class)
class NotificacaoPushServiceTest {

    @Mock private DispositivoPushRepository dispositivoRepository;
    @Mock private ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;
    @InjectMocks private NotificacaoPushService notificacaoPushService;

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void transfereTokenParaUsuarioAtualmenteAutenticado() {
        Usuario usuarioAtual = autenticar("usuario-atual");
        Usuario usuarioAnterior = new Usuario();
        usuarioAnterior.setId("usuario-anterior");

        DispositivoPush existente = new DispositivoPush();
        existente.setUsuario(usuarioAnterior);
        existente.setToken("token-do-celular");
        when(dispositivoRepository.findByToken("token-do-celular")).thenReturn(Optional.of(existente));

        DispositivoPushDTO dto = new DispositivoPushDTO();
        dto.setToken("token-do-celular");
        dto.setPlataforma("ANDROID");
        notificacaoPushService.registrarDispositivo(dto);

        verify(dispositivoRepository).save(existente);
        assertEquals(usuarioAtual, existente.getUsuario());
    }

    @Test
    void removeSomenteTokenDoUsuarioAutenticado() {
        Usuario usuario = autenticar("usuario-1");

        notificacaoPushService.removerDispositivo("token-do-celular");

        verify(dispositivoRepository).deleteByUsuario_IdAndToken(usuario.getId(), "token-do-celular");
    }

    private Usuario autenticar(String id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setAtivo(true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
        return usuario;
    }
}
