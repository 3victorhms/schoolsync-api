package br.cefetmg.schoolsync_api.dto.tarefa;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TarefaStatusDTO {

    @NotBlank
    private String status;

    @NotBlank
    private String idUsuarioLogado;
}
