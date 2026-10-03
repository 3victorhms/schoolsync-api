package br.cefetmg.schoolsync_api.dto.sala;

import br.cefetmg.schoolsync_api.entity.Materia;

public record MateriaResponseDTO(String id, String nome) {

    public MateriaResponseDTO(Materia materia) {
        this(materia.getId(), materia.getNome());
    }
}
