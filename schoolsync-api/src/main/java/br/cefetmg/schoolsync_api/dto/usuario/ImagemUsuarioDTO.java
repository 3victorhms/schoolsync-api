package br.cefetmg.schoolsync_api.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Corpo usado para trocar a foto de perfil.
 * A imagem chega como Data URI em Base64 (ex.: "data:image/png;base64,....")
 * em vez de multipart/form-data, para evitar problemas de proxy/CDN com
 * uploads multipart em alguns ambientes.
 */
@Getter
@Setter
public class ImagemUsuarioDTO {

    @NotBlank(message = "Envie uma imagem para o perfil")
    @Size(max = 7000000, message = "A imagem deve ter no máximo 5 MB")
    private String imagemBase64;
}
