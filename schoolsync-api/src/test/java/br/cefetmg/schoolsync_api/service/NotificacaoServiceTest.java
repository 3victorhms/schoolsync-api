package br.cefetmg.schoolsync_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.cefetmg.schoolsync_api.dto.notificacao.NotificacaoResponseDTO;
import br.cefetmg.schoolsync_api.entity.Notificacao;
import br.cefetmg.schoolsync_api.entity.NotificacaoConfiguracao;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.NotificacaoConfiguracaoRepository;
import br.cefetmg.schoolsync_api.repository.NotificacaoRepository;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {

    @Mock private NotificacaoRepository notificacaoRepository;
    @Mock private NotificacaoConfiguracaoRepository configuracaoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private NotificacaoService notificacaoService;

    @Test
    void atualizaNotificacaoPendenteEmVezDeCriarDuplicada() {
        Usuario usuario = new Usuario();
        usuario.setId("usuario-1");

        NotificacaoConfiguracao configuracao = new NotificacaoConfiguracao();
        configuracao.setUsuario(usuario);
        configuracao.setNoAplicativo(true);

        Notificacao existente = new Notificacao();
        existente.setId("notificacao-1");
        existente.setUsuario(usuario);
        existente.setTipo("ATIVIDADE");
        existente.setTargetId("atividade-1");
        existente.setTitulo("Título anterior");
        existente.setMensagem("Mensagem anterior");
        existente.setHorario(LocalDateTime.now().minusHours(1));
        existente.setLido(false);

        when(configuracaoRepository.findByUsuario_Id(usuario.getId())).thenReturn(Optional.of(configuracao));
        when(notificacaoRepository.findFirstByUsuario_IdAndTipoAndTargetIdAndLidoFalseOrderByHorarioDesc(
                usuario.getId(), "ATIVIDADE", "atividade-1")).thenReturn(Optional.of(existente));
        when(notificacaoRepository.save(existente)).thenReturn(existente);

        NotificacaoResponseDTO resposta = notificacaoService.criarParaUsuario(
                usuario.getId(), "ATIVIDADE", "Atividade atualizada", "Novo prazo", "atividade-1");

        assertEquals("Atividade atualizada", existente.getTitulo());
        assertEquals("Novo prazo", existente.getMensagem());
        assertEquals(existente.getId(), resposta.getId());
        verify(notificacaoRepository).save(existente);
    }
}
