package br.cefetmg.schoolsync_api.dto.pagina;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Feito com auxílio do Claude: regras comuns da paginação das listas (notificações e comentários).
 *
 * A paginação é por cursor: o app manda o horário do item mais antigo que já tem
 * ({@code antesDe}) e recebe os próximos mais antigos. Diferente de "página 2", isso não
 * repete nem pula itens quando chega uma notificação ou um comentário novo no meio do caminho.
 */
public final class Paginacao {

    public static final int TAMANHO_PADRAO = 20;
    public static final int TAMANHO_MAXIMO = 50;

    private Paginacao() {
    }

    /** Tamanho pedido pelo app, limitado entre 1 e {@value #TAMANHO_MAXIMO}. */
    public static int tamanho(Integer pedido) {
        if (pedido == null) {
            return TAMANHO_PADRAO;
        }
        return Math.max(1, Math.min(TAMANHO_MAXIMO, pedido));
    }

    /** Converte o cursor (ex.: 2026-10-25T13:04:05.123) ou devolve null quando não veio. */
    public static LocalDateTime cursor(String antesDe) {
        if (antesDe == null || antesDe.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(antesDe.trim());
        } catch (DateTimeParseException e) {
            // o TratamentoGlobalDeErros transforma em 400
            throw new IllegalArgumentException("Parâmetro antesDe inválido: use o horário do último item recebido");
        }
    }
}
