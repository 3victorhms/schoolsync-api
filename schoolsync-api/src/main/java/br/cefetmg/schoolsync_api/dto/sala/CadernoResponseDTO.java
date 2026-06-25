package br.cefetmg.schoolsync_api.dto.sala;

import br.cefetmg.schoolsync_api.entity.Caderno;
import lombok.Getter;

@Getter
public class CadernoResponseDTO {
    private String idAtividade;
    private String idUsuario;
    private String nomeUsuario;
    private String status;

    public CadernoResponseDTO(Caderno caderno) {
        this.idAtividade = caderno.getAtividade().getId();
        this.idUsuario = caderno.getUsuario().getId();
        this.nomeUsuario = caderno.getUsuario().getNome();
        this.status = caderno.getStatus();
    }
}