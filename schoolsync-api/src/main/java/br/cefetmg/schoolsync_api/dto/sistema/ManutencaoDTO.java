package br.cefetmg.schoolsync_api.dto.sistema;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import br.cefetmg.schoolsync_api.entity.ConfiguracaoSistema;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ManutencaoDTO(
        boolean ativa,
        String mensagem,
        String previsao,
        LocalDateTime alteradoEm,
        String alteradoPor
) {

    public static final String MENSAGEM_PADRAO = "Estamos fazendo uma manutenção no SchoolSync. Volte daqui a pouco.";

    public ManutencaoDTO(ConfiguracaoSistema configuracao) {
        this(configuracao.isManutencao(),
                configuracao.getMensagem() == null || configuracao.getMensagem().isBlank() ? MENSAGEM_PADRAO : configuracao.getMensagem(),
                configuracao.getPrevisao(),
                configuracao.getAlteradoEm(),
                configuracao.getAlteradoPor());
    }

    public static ManutencaoDTO desligada() {
        return new ManutencaoDTO(false, MENSAGEM_PADRAO, null, null, null);
    }

    // o que qualquer pessoa pode ver (sem quem ligou nem quando)
    public ManutencaoDTO publico() {
        return new ManutencaoDTO(ativa, mensagem, previsao, null, null);
    }
}
