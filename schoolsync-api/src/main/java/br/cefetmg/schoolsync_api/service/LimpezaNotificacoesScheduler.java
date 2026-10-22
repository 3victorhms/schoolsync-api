package br.cefetmg.schoolsync_api.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.cefetmg.schoolsync_api.repository.NotificacaoRepository;
import lombok.RequiredArgsConstructor;

// feito com auxílio do Claude
// toda madrugada apaga as notificações já lidas com mais de 30 dias, pra tabela n crescer sem limite.
// as não lidas ficam, mesmo antigas, pq o aluno ainda n viu
@Component
@RequiredArgsConstructor
public class LimpezaNotificacoesScheduler {

    static final int DIAS_PARA_APAGAR = 30;
    private static final Logger log = LoggerFactory.getLogger(LimpezaNotificacoesScheduler.class);

    private final NotificacaoRepository notificacaoRepository;

    @Scheduled(cron = "${notificacoes.limpeza.cron:0 30 3 * * *}", zone = "${notificacoes.lembretes.zone:America/Sao_Paulo}")
    @Transactional
    public void limparNotificacoesAntigas() {
        long apagadas = notificacaoRepository.deleteByLidoTrueAndHorarioBefore(LocalDateTime.now().minusDays(DIAS_PARA_APAGAR));
        if (apagadas > 0) {
            log.info("Limpeza: {} notificações lidas com mais de {} dias foram apagadas", apagadas, DIAS_PARA_APAGAR);
        }
    }
}
