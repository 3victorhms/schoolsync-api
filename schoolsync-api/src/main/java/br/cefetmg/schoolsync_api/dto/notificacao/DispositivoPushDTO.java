package br.cefetmg.schoolsync_api.dto.notificacao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DispositivoPushDTO {

    @NotBlank(message = "O token do dispositivo é obrigatório")
    @Size(max = 512, message = "Token de dispositivo inválido")
    private String token;

    @NotBlank(message = "A plataforma é obrigatória")
    @Pattern(regexp = "ANDROID|IOS", message = "Plataforma de dispositivo inválida")
    private String plataforma;
}
