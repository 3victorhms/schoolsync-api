package br.cefetmg.schoolsync_api.dto.nota;

import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Lançar ou editar uma nota do boletim.
 * Com idAtividade: só valorObtido importa (o resto vem da atividade).
 * Sem idAtividade (avulsa): descricao, idMateria, valorMaximo e data são obrigatórios.
 */
@Getter
@Setter
public class NotaRequestDTO {

    private String idAtividade;

    @NotNull(message = "Informe a nota obtida")
    @DecimalMin(value = "0.0", message = "A nota não pode ser negativa")
    private Double valorObtido;

    // ===== só para nota avulsa =====

    @Size(max = 150, message = "A descrição deve ter no máximo 150 caracteres")
    private String descricao;

    private String idMateria;

    private Double valorMaximo;

    private LocalDate data;
}
