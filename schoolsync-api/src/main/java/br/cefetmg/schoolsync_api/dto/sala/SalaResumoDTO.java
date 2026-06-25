package br.cefetmg.schoolsync_api.dto.sala;

import br.cefetmg.schoolsync_api.entity.Sala;
import lombok.Getter;

@Getter
// classe criada para não ter que retornar todas salas e atividades da sala toda vez que alguma sala for necessário retornar uma sala
// será usada na tela salas do front, enquanto a tela de alguma sala específica receberá SalaResponseDTO
public class SalaResumoDTO {

    private String id;
    private String nome;
    private String codigoConvite;
    private String idLider;
    private int quantidadeMembros;
    private int quantidadeAtividades;

    public SalaResumoDTO(Sala sala) {
        this.id = sala.getId();
        this.nome = sala.getNome();
        this.codigoConvite = sala.getCodigoConvite();
        this.idLider = sala.getLider().getId();
        this.quantidadeMembros = sala.getMembros().size();
        this.quantidadeAtividades = sala.getAtividades().size();
    }
}