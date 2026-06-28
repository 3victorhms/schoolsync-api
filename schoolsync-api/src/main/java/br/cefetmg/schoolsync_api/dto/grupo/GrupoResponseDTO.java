package br.cefetmg.schoolsync_api.dto.grupo;

import java.util.List;
import java.util.stream.Collectors;

import br.cefetmg.schoolsync_api.dto.sala.MembrosResponseDTO;
import br.cefetmg.schoolsync_api.dto.tarefa.TarefaResponseDTO;
import br.cefetmg.schoolsync_api.entity.Grupo;
import br.cefetmg.schoolsync_api.entity.Membros;
import lombok.Getter;

@Getter
public class GrupoResponseDTO {

    private String id;
    private String nome;
    private String idSala;
    private String nomeSala;
    private String idCriador;
    private String nomeCriador;
    private String codigoConvite;
    private boolean criador;
    private List<MembroGrupoResponseDTO> membros;
    private List<TarefaResponseDTO> tarefas;

    public GrupoResponseDTO(Grupo grupo, String idUsuarioLogado) {
        this.id = grupo.getId();
        this.nome = grupo.getNome();
        this.idSala = grupo.getSala().getId();
        this.nomeSala = grupo.getSala().getNome();
        this.idCriador = grupo.getCriador().getId();
        this.nomeCriador = grupo.getCriador().getNome();
        this.codigoConvite = grupo.getCodigoConvite();
        this.criador = grupo.getCriador().getId().equals(idUsuarioLogado);
        this.membros = grupo.getMembros().stream()
                .map(membro -> new MembroGrupoResponseDTO(membro, grupo.getCriador().getId()))
                .collect(Collectors.toList());
        this.tarefas = grupo.getTarefas().stream()
                .map(TarefaResponseDTO::new)
                .collect(Collectors.toList());
    }
}
