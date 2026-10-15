package br.cefetmg.schoolsync_api.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.cefetmg.schoolsync_api.entity.Atividade;
import br.cefetmg.schoolsync_api.entity.Caderno;
import br.cefetmg.schoolsync_api.entity.NotificacaoConfiguracao;
import br.cefetmg.schoolsync_api.repository.CadernoRepository;
import br.cefetmg.schoolsync_api.repository.NotificacaoConfiguracaoRepository;
import br.cefetmg.schoolsync_api.repository.NotificacaoRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LembreteAtividadeScheduler {

    private static final String TIPO_LEMBRETE = "LEMBRETE_ATIVIDADE";
    private static final List<Integer> DIAS_PERMITIDOS = List.of(1, 3, 7);

    private final CadernoRepository cadernoRepository;
    private final NotificacaoConfiguracaoRepository configuracaoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final NotificacaoService notificacaoService;

    @Scheduled(cron = "${notificacoes.lembretes.cron:0 0 9 * * *}", zone = "${notificacoes.lembretes.zone:America/Sao_Paulo}")
    @Transactional
    public void enviarLembretesDiarios() {
        enviarLembretesPara(LocalDate.now());
    }

    void enviarLembretesPara(LocalDate hoje) {
        for (Integer diasAntes : DIAS_PERMITIDOS) {
            LocalDate dataEntrega = hoje.plusDays(diasAntes);
            for (Caderno caderno : cadernoRepository.findByAtividade_DataEntrega(dataEntrega)) {
                enviarSeNecessario(caderno, diasAntes);
            }
        }
    }

    private void enviarSeNecessario(Caderno caderno, int diasAntes) {
        if (caderno.getUsuario() == null || !caderno.getUsuario().isAtivo()) {
            return;
        }

        // n faz sentido lembrar do prazo de uma atividade q a pessoa já concluiu
        if ("concluido".equals(caderno.getStatus())) {
            return;
        }

        int preferencia = configuracaoRepository.findByUsuario_Id(caderno.getUsuario().getId())
                .map(NotificacaoConfiguracao::getLembreteDias)
                .orElse(3);
        if (preferencia != diasAntes) {
            return;
        }

        Atividade atividade = caderno.getAtividade();
        String targetId = String.format("%s:%s:%d", atividade.getId(), atividade.getDataEntrega(), diasAntes);
        if (notificacaoRepository.existsByUsuario_IdAndTipoAndTargetId(
                caderno.getUsuario().getId(), TIPO_LEMBRETE, targetId)) {
            return;
        }

        notificacaoService.criarParaUsuario(
                caderno.getUsuario().getId(),
                TIPO_LEMBRETE,
                "Lembrete de atividade",
                String.format("%s vence em %d %s.", atividade.getTitulo(), diasAntes,
                        diasAntes == 1 ? "dia" : "dias"),
                targetId);
    }
}
