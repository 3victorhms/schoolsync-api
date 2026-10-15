package br.cefetmg.schoolsync_api.dto.tarefa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TarefaStatusDTO {

    @NotBlank
    @Pattern(regexp = "pendente|em_andamento|concluido", message = "Status inválido: use pendente, em_andamento ou concluido")
    private String status;
}
