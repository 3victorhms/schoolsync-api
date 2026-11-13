package br.cefetmg.schoolsync_api.dto.pagina;

import java.util.List;

public record PaginaDTO<T>(List<T> itens, boolean temMais) {
}
