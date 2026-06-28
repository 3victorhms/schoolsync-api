package br.cefetmg.schoolsync_api.dto.comentario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComentarioUpdateDTO {

    @NotBlank
    @Size(max = 1000)
    private String texto;

    @NotBlank
    private String idUsuario;
}
