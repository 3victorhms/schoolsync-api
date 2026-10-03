package br.cefetmg.schoolsync_api.dto.sala;

import java.util.ArrayList;
import java.util.List;

import br.cefetmg.schoolsync_api.entity.TipoPeriodo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Usado para criar e para editar a sala. Na edição a lista de matérias é a lista
 * final: as que têm id são mantidas/renomeadas, as sem id são criadas e as que
 * sumiram da lista são removidas (se não tiverem atividades).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalaRequestDTO {

    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    private String nome;

    @NotEmpty(message = "Cadastre pelo menos uma matéria")
    @Size(max = 30, message = "Uma sala pode ter no máximo 30 matérias")
    @Valid
    private List<MateriaRequestDTO> materias = new ArrayList<>();

    @NotNull(message = "Escolha como o ano é dividido: bimestre, trimestre ou semestre")
    private TipoPeriodo tipoPeriodo;

    @NotEmpty(message = "Informe os períodos letivos")
    @Valid
    private List<PeriodoRequestDTO> periodos = new ArrayList<>();
}
