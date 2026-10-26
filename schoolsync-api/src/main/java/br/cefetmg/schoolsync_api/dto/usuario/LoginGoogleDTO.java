package br.cefetmg.schoolsync_api.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginGoogleDTO {

    // ID token (JWT) que o app recebe do google depois que a pessoa escolhe a conta
    @NotBlank(message = "Faltou o token do Google")
    private String idToken;
}
