package br.cefetmg.schoolsync_api.dto.pagina;

import java.util.List;

/**
 * Feito com auxílio do Claude: uma página de resultados.
 * {@code temMais} diz se ainda há itens mais antigos para buscar com o cursor {@code antesDe}.
 */
public record PaginaDTO<T>(List<T> itens, boolean temMais) {
}
