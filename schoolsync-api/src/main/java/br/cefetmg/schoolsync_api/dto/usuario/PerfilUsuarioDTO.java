package br.cefetmg.schoolsync_api.dto.usuario;

import br.cefetmg.schoolsync_api.entity.Perfil;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// corpo do PUT /admin/usuarios/{id}/perfil -> { "perfil": "ADMIN" } ou { "perfil": "USUARIO" }
@Getter
@Setter
public class PerfilUsuarioDTO {

    @NotNull(message = "Informe o perfil: USUARIO ou ADMIN")
    private Perfil perfil;
}
