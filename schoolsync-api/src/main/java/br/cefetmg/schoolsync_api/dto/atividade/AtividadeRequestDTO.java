package br.cefetmg.schoolsync_api.dto.atividade;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
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
    private Double valor;

    @NotBlank
    private String idSala;

    @NotBlank
    private String idCriador;
}