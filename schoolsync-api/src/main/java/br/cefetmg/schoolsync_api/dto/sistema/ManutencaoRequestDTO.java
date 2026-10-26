package br.cefetmg.schoolsync_api.dto.sistema;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ManutencaoRequestDTO {

    private boolean ativa;

    @Size(max = 300, message = "A mensagem deve ter no máximo 300 caracteres")
    private String mensagem;

    @Size(max = 60, message = "A previsão deve ter no máximo 60 caracteres")
    private String previsao;
}
