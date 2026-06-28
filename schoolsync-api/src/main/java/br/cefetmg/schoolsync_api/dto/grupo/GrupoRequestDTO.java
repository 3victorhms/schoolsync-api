package br.cefetmg.schoolsync_api.dto.grupo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GrupoRequestDTO {

    @NotBlank
    @Size(max = 150)
    private String nome;

    @NotBlank
    private String idSala;

    @NotBlank
    private String idCriador;
}
