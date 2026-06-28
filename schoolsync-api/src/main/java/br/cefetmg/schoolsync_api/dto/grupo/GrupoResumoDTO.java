package br.cefetmg.schoolsync_api.dto.grupo;

import br.cefetmg.schoolsync_api.entity.Grupo;
import lombok.Getter;

@Getter
public class GrupoResumoDTO {

    private String id;
    private String nome;
    private String idSala;
    private String nomeSala;
    private String idCriador;
    private String codigoConvite;
    private int quantidadeMembros;
    private int quantidadeTarefas;
    private boolean criador;

    public GrupoResumoDTO(Grupo grupo, String idUsuarioLogado) {
        this.id = grupo.getId();
        this.nome = grupo.getNome();
        this.idSala = grupo.getSala().getId();
        this.nomeSala = grupo.getSala().getNome();
        this.idCriador = grupo.getCriador().getId();
        this.codigoConvite = grupo.getCodigoConvite();
        this.quantidadeMembros = grupo.getMembros().size();
        this.quantidadeTarefas = grupo.getTarefas().size();
        this.criador = grupo.getCriador().getId().equals(idUsuarioLogado);
    }
}
