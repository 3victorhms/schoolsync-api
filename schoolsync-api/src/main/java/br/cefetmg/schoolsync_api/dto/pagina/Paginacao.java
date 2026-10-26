package br.cefetmg.schoolsync_api.dto.pagina;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

// regras da paginação de notificações e comentários. é por cursor: o app manda o horário do
// item mais antigo que já tem (antesDe) e recebe os anteriores. diferente de "página 2", n repete
// nem pula item quando chega notificação ou comentário novo no meio
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

    // converte o cursor (ex.: 2026-10-25T13:04:05.123) ou devolve null quando não veio
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
