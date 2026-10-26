package br.cefetmg.schoolsync_api.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.cefetmg.schoolsync_api.dto.sistema.ManutencaoDTO;
import br.cefetmg.schoolsync_api.dto.sistema.ManutencaoRequestDTO;
import br.cefetmg.schoolsync_api.entity.ConfiguracaoSistema;
import br.cefetmg.schoolsync_api.repository.ConfiguracaoSistemaRepository;

// modo manutenção: o admin liga e todo mundo que n é admin passa a receber 503 (ManutencaoFilter).
// o filtro roda em toda requisição, ent o estado fica guardado em memória e só é relido do banco
// a cada 20 s (se um dia tiver mais de uma instância da API, elas se acertam nesse tempo)
@Service
public class ManutencaoService {

    static final String ID = "geral";
    private static final Duration VALIDADE = Duration.ofSeconds(20);

    private final Logger log = LoggerFactory.getLogger(ManutencaoService.class);
    private final ConfiguracaoSistemaRepository repository;
    private final Clock relogio;

    private volatile ManutencaoDTO estado;
    private volatile Instant lidoEm;

    @Autowired
    public ManutencaoService(ConfiguracaoSistemaRepository repository) {
        this(repository, Clock.systemUTC());
    }

    ManutencaoService(ConfiguracaoSistemaRepository repository, Clock relogio) {
        this.repository = repository;
        this.relogio = relogio;
    }

    public boolean ativa() {
        return estado().ativa();
    }

    public ManutencaoDTO estado() {
        Instant agora = relogio.instant();
        ManutencaoDTO atual = estado;
        if (atual != null && lidoEm != null && lidoEm.plus(VALIDADE).isAfter(agora)) {
            return atual;
        }
        try {
            atual = repository.findById(ID).map(ManutencaoDTO::new).orElseGet(ManutencaoDTO::desligada);
        } catch (RuntimeException e) {
            // banco fora do ar: mantém o último estado conhecido em vez de derrubar todas as requisições
            log.warn("Não deu pra ler o modo manutenção: {}", e.getMessage());
            atual = atual != null ? atual : ManutencaoDTO.desligada();
        }
        estado = atual;
        lidoEm = agora;
        return atual;
    }

    @Transactional
    public ManutencaoDTO atualizar(ManutencaoRequestDTO dto, String nomeAdmin) {
        ConfiguracaoSistema configuracao = repository.findById(ID).orElseGet(() -> {
            ConfiguracaoSistema nova = new ConfiguracaoSistema();
            nova.setId(ID);
            return nova;
        });
        configuracao.setManutencao(dto.isAtiva());
        configuracao.setMensagem(vazioViraNull(dto.getMensagem()));
        configuracao.setPrevisao(vazioViraNull(dto.getPrevisao()));
        configuracao.setAlteradoEm(LocalDateTime.now());
        configuracao.setAlteradoPor(nomeAdmin);

        ManutencaoDTO salvo = new ManutencaoDTO(repository.save(configuracao));
        estado = salvo;
        lidoEm = relogio.instant();
        log.info("Modo manutenção {} por {}", salvo.ativa() ? "ligado" : "desligado", nomeAdmin);
        return salvo;
    }

    private static String vazioViraNull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
