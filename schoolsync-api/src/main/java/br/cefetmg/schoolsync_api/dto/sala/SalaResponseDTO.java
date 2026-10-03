package br.cefetmg.schoolsync_api.dto.sala;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import br.cefetmg.schoolsync_api.entity.Sala;
import br.cefetmg.schoolsync_api.entity.TipoPeriodo;
import lombok.Getter;

@Getter
public class SalaResponseDTO {
    private String id;
    private String nome;
    private String codigoConvite;
    private String idLider;
    private TipoPeriodo tipoPeriodo;
    private List<MateriaResponseDTO> materias;
    private List<PeriodoResponseDTO> periodos;
    private List<MembrosResponseDTO> membros;
    private List<AtividadeResumo> atividades;

    public SalaResponseDTO(Sala sala, Map<String, String> statusPorAtividade) {
        this.id = sala.getId();
        this.nome = sala.getNome();
        this.codigoConvite = sala.getCodigoConvite();
        this.idLider = sala.getLider().getId();
        this.tipoPeriodo = sala.getTipoPeriodo();

        this.materias = sala.getMaterias().stream()
                .map(MateriaResponseDTO::new)
                .collect(Collectors.toList());

        this.periodos = sala.getPeriodos().stream()
                .map(PeriodoResponseDTO::new)
                .collect(Collectors.toList());

        this.membros = sala.getMembros().stream()
                .map(MembrosResponseDTO::new)
                .collect(Collectors.toList());

        this.atividades = sala.getAtividades().stream()
                .map(a -> {
                    String status = statusPorAtividade.get(a.getId());
                    return new AtividadeResumo(
                            a.getId(),
                            a.getTitulo(),
                            a.getDescricao(),
                            a.getMateria().getId(),
                            a.getMateria().getNome(),
                            a.getPeriodo().getId(),
                            a.getPeriodo().getNome(),
                            a.getDataEntrega(),
                            a.getValor(),
                            status != null,
                            status
                    );
                })
                .collect(Collectors.toList());
    }

    public record AtividadeResumo(
            String id,
            String titulo,
            String descricao,
            String idMateria,
            String nomeMateria,
            String idPeriodo,
            String nomePeriodo,
            LocalDate dataEntrega,
            Double valor,
            boolean estaNoCaderno,
            String status
    ) {}
}
