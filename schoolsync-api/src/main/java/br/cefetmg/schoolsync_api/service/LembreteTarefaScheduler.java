package br.cefetmg.schoolsync_api.service;

import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.cefetmg.schoolsync_api.entity.Tarefa;
import br.cefetmg.schoolsync_api.repository.NotificacaoRepository;
import br.cefetmg.schoolsync_api.repository.TarefaRepository;
import lombok.RequiredArgsConstructor;

// todo dia de manhã avisa quem está com uma tarefa de grupo q vence amanhã (e ainda n concluiu)
@Component
@RequiredArgsConstructor
public class LembreteTarefaScheduler {

    // começa com GRUPO pra o app abrir a tela do grupo ao tocar na notificação
    static final String TIPO_LEMBRETE = "GRUPO_LEMBRETE_TAREFA";

    private final TarefaRepository tarefaRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final NotificacaoService notificacaoService;

    @Scheduled(cron = "${notificacoes.lembretes.cron:0 0 9 * * *}", zone = "${notificacoes.lembretes.zone:America/Sao_Paulo}")
    @Transactional
    public void enviarLembretesDiarios() {
        enviarLembretesPara(LocalDate.now());
    }

    void enviarLembretesPara(LocalDate hoje) {
        for (Tarefa tarefa : tarefaRepository.findByPrazoAndStatusNot(hoje.plusDays(1), "concluido")) {
            if (tarefa.getAtribuidoPara() == null || !tarefa.getAtribuidoPara().isAtivo()) {
                continue;
            }

            // o alvo é "idGrupo:pedaço do id da tarefa" (cabe nos 64 caracteres do target_id e n repete o lembrete)
            String idTarefa = tarefa.getId().replace("-", "");
            String targetId = tarefa.getGrupo().getId() + ":" + idTarefa.substring(0, Math.min(24, idTarefa.length()));
            String idUsuario = tarefa.getAtribuidoPara().getId();
            if (notificacaoRepository.existsByUsuario_IdAndTipoAndTargetId(idUsuario, TIPO_LEMBRETE, targetId)) {
                continue;
            }

            notificacaoService.criarParaUsuario(
                    idUsuario,
                    TIPO_LEMBRETE,
                    "Sua parte vence amanhã",
                    String.format("\"%s\" (%s) vence amanhã.", tarefa.getTitulo(), tarefa.getGrupo().getNome()),
                    targetId);
        }
    }
}
