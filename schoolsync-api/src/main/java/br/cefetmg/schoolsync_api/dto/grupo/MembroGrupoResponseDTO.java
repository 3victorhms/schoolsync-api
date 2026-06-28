package br.cefetmg.schoolsync_api.dto.grupo;

import br.cefetmg.schoolsync_api.entity.GrupoMembro;
import lombok.Getter;

@Getter
public class MembroGrupoResponseDTO {

    private String idUsuario;
    private String nomeUsuario;
    private String fotoUsuario;
    private boolean criador;

    public MembroGrupoResponseDTO(GrupoMembro membro, String idCriador) {
        this.idUsuario = membro.getUsuario().getId();
        this.nomeUsuario = membro.getUsuario().getNome();
        this.fotoUsuario = membro.getUsuario().getFoto();
        this.criador = membro.getUsuario().getId().equals(idCriador);
    }
}
