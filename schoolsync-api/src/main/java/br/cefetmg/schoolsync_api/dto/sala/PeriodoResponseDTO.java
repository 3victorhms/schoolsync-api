package br.cefetmg.schoolsync_api.dto.sala;

import java.time.LocalDate;

import br.cefetmg.schoolsync_api.entity.Periodo;

public record PeriodoResponseDTO(
        String id,
        Integer ordem,
        String nome,
        LocalDate dataInicio,
        LocalDate dataFim,
        Double pontuacaoMaxima
) {

    public PeriodoResponseDTO(Periodo periodo) {
        this(
                periodo.getId(),
                periodo.getOrdem(),
                periodo.getNome(),
                periodo.getDataInicio(),
                periodo.getDataFim(),
                periodo.getPontuacaoMaxima()
        );
    }
}
