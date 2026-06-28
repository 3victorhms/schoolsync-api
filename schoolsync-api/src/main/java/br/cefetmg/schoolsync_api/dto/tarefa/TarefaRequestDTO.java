package br.cefetmg.schoolsync_api.dto.tarefa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TarefaRequestDTO {

    @NotBlank
    @Size(max = 150)
    private String titulo;

    @NotBlank
    private String idAtividade;

    @NotBlank
    private String idUsuarioAtribuido;

    @NotBlank
    private String idUsuarioLogado;
}
