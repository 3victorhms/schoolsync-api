package br.cefetmg.schoolsync_api.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.cefetmg.schoolsync_api.entity.Atividade;
import br.cefetmg.schoolsync_api.entity.Caderno;
import br.cefetmg.schoolsync_api.entity.NotificacaoConfiguracao;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.CadernoRepository;
import br.cefetmg.schoolsync_api.repository.NotificacaoConfiguracaoRepository;
import br.cefetmg.schoolsync_api.repository.NotificacaoRepository;

@ExtendWith(MockitoExtension.class)
class LembreteAtividadeSchedulerTest {

    @Mock private CadernoRepository cadernoRepository;
    @Mock private NotificacaoConfiguracaoRepository configuracaoRepository;
    @Mock private NotificacaoRepository notificacaoRepository;
    @Mock private NotificacaoService notificacaoService;
    @InjectMocks private LembreteAtividadeScheduler scheduler;

    @Test
    void enviaLembreteNaDataDaPreferenciaDoUsuario() {
        LocalDate hoje = LocalDate.of(2026, 9, 16);
        Usuario usuario = usuarioAtivo();
        Atividade atividade = atividade("atividade-1", "Prova de Matemática", hoje.plusDays(3));
        Caderno caderno = caderno(usuario, atividade);
        NotificacaoConfiguracao configuracao = new NotificacaoConfiguracao();
        configuracao.setLembreteDias(3);

        when(cadernoRepository.findByAtividade_DataEntrega(hoje.plusDays(1))).thenReturn(List.of());
        when(cadernoRepository.findByAtividade_DataEntrega(hoje.plusDays(3))).thenReturn(List.of(caderno));
        when(cadernoRepository.findByAtividade_DataEntrega(hoje.plusDays(7))).thenReturn(List.of());
        when(configuracaoRepository.findByUsuario_Id("usuario-1")).thenReturn(Optional.of(configuracao));
        when(notificacaoRepository.existsByUsuario_IdAndTipoAndTargetId(
                "usuario-1", "LEMBRETE_ATIVIDADE", "atividade-1:2026-09-19:3")).thenReturn(false);

        scheduler.enviarLembretesPara(hoje);

        verify(notificacaoService).criarParaUsuario(
                "usuario-1", "LEMBRETE_ATIVIDADE", "Lembrete de atividade",
                "Prova de Matemática vence em 3 dias.", "atividade-1:2026-09-19:3");
    }

    @Test
    void naoReenviaLembreteJaRegistrado() {
        LocalDate hoje = LocalDate.of(2026, 9, 16);
        Usuario usuario = usuarioAtivo();
        Atividade atividade = atividade("atividade-1", "Trabalho", hoje.plusDays(1));
        Caderno caderno = caderno(usuario, atividade);
        NotificacaoConfiguracao configuracao = new NotificacaoConfiguracao();
        configuracao.setLembreteDias(1);

        when(cadernoRepository.findByAtividade_DataEntrega(hoje.plusDays(1))).thenReturn(List.of(caderno));
        when(cadernoRepository.findByAtividade_DataEntrega(hoje.plusDays(3))).thenReturn(List.of());
        when(cadernoRepository.findByAtividade_DataEntrega(hoje.plusDays(7))).thenReturn(List.of());
        when(configuracaoRepository.findByUsuario_Id("usuario-1")).thenReturn(Optional.of(configuracao));
        when(notificacaoRepository.existsByUsuario_IdAndTipoAndTargetId(anyString(), anyString(), anyString()))
                .thenReturn(true);

        scheduler.enviarLembretesPara(hoje);

        verify(notificacaoService, never()).criarParaUsuario(
                anyString(), anyString(), anyString(), anyString(), anyString());
    }

    private Usuario usuarioAtivo() {
        Usuario usuario = new Usuario();
        usuario.setId("usuario-1");
        usuario.setAtivo(true);
        return usuario;
    }

    private Atividade atividade(String id, String titulo, LocalDate dataEntrega) {
        Atividade atividade = new Atividade();
        atividade.setId(id);
        atividade.setTitulo(titulo);
        atividade.setDataEntrega(dataEntrega);
        return atividade;
    }

    private Caderno caderno(Usuario usuario, Atividade atividade) {
        Caderno caderno = new Caderno();
        caderno.setUsuario(usuario);
        caderno.setAtividade(atividade);
        return caderno;
    }
}
