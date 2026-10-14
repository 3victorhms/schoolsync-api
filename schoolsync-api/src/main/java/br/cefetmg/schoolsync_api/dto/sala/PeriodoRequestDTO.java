package br.cefetmg.schoolsync_api.dto.sala;

import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PeriodoRequestDTO {

    @NotNull(message = "Informe a data de início do período")
    private LocalDate dataInicio;

    @NotNull(message = "Informe a data de fim do período")
    private LocalDate dataFim;

    @NotNull(message = "Informe a pontuação máxima do período")
    @DecimalMin(value = "0.01", message = "A pontuação máxima do período deve ser maior que zero")
    @DecimalMax(value = "100.0", message = "A pontuação máxima do período é 100 pontos")
    private Double pontuacaoMaxima;
}
