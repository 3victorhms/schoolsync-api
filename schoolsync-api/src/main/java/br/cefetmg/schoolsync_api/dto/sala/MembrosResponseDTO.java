package br.cefetmg.schoolsync_api.dto.sala;

import br.cefetmg.schoolsync_api.entity.Membros;
import lombok.Getter;

@Getter
public class MembrosResponseDTO {
    private String idUsuario;
    private String nomeUsuario;

    public MembrosResponseDTO(Membros membro) {
        this.idUsuario = membro.getUsuario().getId();
        this.nomeUsuario = membro.getUsuario().getNome();
    }
}