package br.cefetmg.schoolsync_api.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImagemUsuarioDTO {

    @NotBlank(message = "Envie uma imagem para o perfil")
    @Size(max = 7000000, message = "A imagem deve ter no máximo 5 MB")
    private String imagemBase64;
}
