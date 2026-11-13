package br.cefetmg.schoolsync_api.dto.pagina;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public final class Paginacao {

    public static final int TAMANHO_PADRAO = 20;
    public static final int TAMANHO_MAXIMO = 50;

    private Paginacao() {
    }

    // tamanho pedido pelo app, entre 1 e TAMANHO_MAXIMO
    public static int tamanho(Integer pedido) {
        if (pedido == null) {
            return TAMANHO_PADRAO;
        }
        return Math.max(1, Math.min(TAMANHO_MAXIMO, pedido));
    }

    public static LocalDateTime cursor(String antesDe) {
        if (antesDe == null || antesDe.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(antesDe.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Parâmetro antesDe inválido: use o horário do último item recebido");
        }
    }
}
