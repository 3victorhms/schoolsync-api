package br.cefetmg.schoolsync_api.exception;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Formato único de erro da API. O app lê sempre "message" para mostrar o aviso;
 * "campos" só aparece em erro de validação (campo -> mensagem).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponseDTO(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> campos
) {}
