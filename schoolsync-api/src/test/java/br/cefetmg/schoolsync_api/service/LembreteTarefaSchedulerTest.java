package br.cefetmg.schoolsync_api.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.cefetmg.schoolsync_api.entity.Grupo;
import br.cefetmg.schoolsync_api.entity.Tarefa;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.NotificacaoRepository;
import br.cefetmg.schoolsync_api.repository.TarefaRepository;

@ExtendWith(MockitoExtension.class)
class LembreteTarefaSchedulerTest {

    @Mock private TarefaRepository tarefaRepository;
    @Mock private NotificacaoRepository notificacaoRepository;
    @Mock private NotificacaoService notificacaoService;
    @InjectMocks private LembreteTarefaScheduler scheduler;

    private Tarefa tarefaDaAna(LocalDate prazo) {
        Usuario ana = new Usuario();
        ana.setId("ana");
        ana.setAtivo(true);

        Grupo grupo = new Grupo();
        grupo.setId("grupo-1");
        grupo.setNome("Seminário");

        Tarefa tarefa = new Tarefa();
        tarefa.setId("11111111-2222-3333-4444-555555555555");
        tarefa.setTitulo("Slides");
        tarefa.setStatus("pendente");
        tarefa.setPrazo(prazo);
        tarefa.setGrupo(grupo);
        tarefa.setAtribuidoPara(ana);
        return tarefa;
    }

    @Test
    void avisaQuemEstaComATarefaUmDiaAntesDoPrazo() {
        LocalDate hoje = LocalDate.of(2026, 10, 22);
        when(tarefaRepository.findByPrazoAndStatusNot(hoje.plusDays(1), "concluido"))
                .thenReturn(List.of(tarefaDaAna(hoje.plusDays(1))));

        scheduler.enviarLembretesPara(hoje);

        verify(notificacaoService).criarParaUsuario(eq("ana"), eq(LembreteTarefaScheduler.TIPO_LEMBRETE),
                anyString(), anyString(), startsWith("grupo-1:"));
    }

    @Test
    void naoRepeteOLembrete() {
        LocalDate hoje = LocalDate.of(2026, 10, 22);
        when(tarefaRepository.findByPrazoAndStatusNot(hoje.plusDays(1), "concluido"))
                .thenReturn(List.of(tarefaDaAna(hoje.plusDays(1))));
        when(notificacaoRepository.existsByUsuario_IdAndTipoAndTargetId(eq("ana"), eq(LembreteTarefaScheduler.TIPO_LEMBRETE), anyString()))
                .thenReturn(true);

        scheduler.enviarLembretesPara(hoje);

        verify(notificacaoService, never()).criarParaUsuario(anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
