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

    @NotBlank
    private String disciplina;

    @NotNull
    private LocalDate dataEntrega;

    @NotNull
    @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
    @DecimalMax(value = "15.0", message = "O valor máximo de uma atividade é 15 pontos")
    private Double valor;

    @NotBlank
    private String idSala;

    @NotBlank
    private String idCriador;
}
