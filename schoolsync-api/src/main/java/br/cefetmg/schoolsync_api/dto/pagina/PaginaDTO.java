package br.cefetmg.schoolsync_api.dto.pagina;

import java.util.List;

// uma página de resultados. temMais diz se ainda tem itens mais antigos pra buscar com o antesDe
public record PaginaDTO<T>(List<T> itens, boolean temMais) {
}
