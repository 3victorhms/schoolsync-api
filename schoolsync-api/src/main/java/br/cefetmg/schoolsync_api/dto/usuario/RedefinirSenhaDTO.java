package br.cefetmg.schoolsync_api.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RedefinirSenhaDTO {

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Size(max = 200, message = "O e-mail deve ter no máximo 200 caracteres")
    private String email;

    @NotBlank(message = "O código é obrigatório")
    @Pattern(regexp = "\\s*\\d{6}\\s*", message = "O código tem 6 dígitos")
    private String codigo;

    // mesma regra do cadastro (UsuarioRequestDTO)
    @NotBlank(message = "A nova senha é obrigatória")
    @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
    private String novaSenha;
}
