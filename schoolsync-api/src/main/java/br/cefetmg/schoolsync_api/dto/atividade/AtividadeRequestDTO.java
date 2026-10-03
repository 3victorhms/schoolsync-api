package br.cefetmg.schoolsync_api.dto.atividade;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AtividadeRequestDTO {

    @NotBlank
    private String titulo;

    private String descricao;

    /** v2: id de uma das matérias da sala (antes era o nome da disciplina digitado). */
    @NotBlank(message = "Escolha a matéria da atividade")
    private String idMateria;

    @NotNull
    private LocalDate dataEntrega;

    @NotNull
    @DecimalMin(value = "0.0", message = "O valor não pode ser negativo")
    @DecimalMax(value = "15.0", message = "O valor máximo de uma atividade é 15 pontos")
    private Double valor;

    @NotBlank
    private String idSala;
}
