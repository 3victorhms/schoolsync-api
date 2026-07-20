package br.cefetmg.schoolsync_api.dto.usuario;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDTO {

    private String token;
    private UsuarioResponseDTO usuario;
}
