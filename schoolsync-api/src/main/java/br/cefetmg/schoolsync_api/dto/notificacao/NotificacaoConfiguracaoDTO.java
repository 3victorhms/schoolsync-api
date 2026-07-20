package br.cefetmg.schoolsync_api.dto.notificacao;

import br.cefetmg.schoolsync_api.entity.NotificacaoConfiguracao;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NotificacaoConfiguracaoDTO {

    private boolean noAplicativo = true;
    private boolean push = false;

    @Min(1)
    @Max(7)
    private Integer lembreteDias = 3;

    public NotificacaoConfiguracaoDTO(NotificacaoConfiguracao configuracao) {
        this.noAplicativo = configuracao.isNoAplicativo();
        this.push = configuracao.isPush();
        this.lembreteDias = configuracao.getLembreteDias();
    }
}
