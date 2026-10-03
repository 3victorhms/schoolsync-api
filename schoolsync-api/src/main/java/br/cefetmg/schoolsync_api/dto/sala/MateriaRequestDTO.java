package br.cefetmg.schoolsync_api.dto.sala;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MateriaRequestDTO {

    /** Vazio para matéria nova; preenchido quando o líder renomeia uma existente. */
    private String id;

    @NotBlank(message = "Informe o nome da matéria")
    @Size(max = 100, message = "O nome da matéria deve ter no máximo 100 caracteres")
    private String nome;
}
