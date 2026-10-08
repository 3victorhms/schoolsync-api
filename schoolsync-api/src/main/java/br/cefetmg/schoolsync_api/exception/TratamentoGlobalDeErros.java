package br.cefetmg.schoolsync_api.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Converte as exceções dos services em respostas com status certo e mensagem legível.
 * Antes, IllegalArgumentException e EntityNotFoundException chegavam no app como 500.
 */
@RestControllerAdvice
public class TratamentoGlobalDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratamentoGlobalDeErros.class);

    /** Regras de negócio lançadas com status explícito (403, 409, 422...). */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroResponseDTO> statusExplicito(ResponseStatusException ex, HttpServletRequest request) {
        String mensagem = ex.getReason() != null ? ex.getReason() : "Não foi possível concluir a ação";
        return resposta(ex.getStatusCode(), mensagem, request, null);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErroResponseDTO> naoEncontrado(EntityNotFoundException ex, HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    /** Dado inválido enviado pelo app (ex.: usuário que não pertence ao grupo). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponseDTO> argumentoInvalido(IllegalArgumentException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    /** @Valid nos DTOs: devolve a primeira mensagem em "message" e todas em "campos". */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponseDTO> validacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        String mensagem = campos.isEmpty()
                ? "Dados inválidos"
                : campos.values().iterator().next();
        return resposta(HttpStatus.BAD_REQUEST, mensagem, request, campos);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErroResponseDTO> requisicaoMalFormada(Exception ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição inválida: confira os dados enviados", request, null);
    }

    /** Upload acima do limite do multipart (spring.servlet.multipart: 5 MB). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErroResponseDTO> arquivoGrandeDemais(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return resposta(HttpStatus.PAYLOAD_TOO_LARGE, "O arquivo deve ter no máximo 5 MB", request, null);
    }

    /** Rota de upload chamada sem o arquivo, ou sem ser multipart/form-data. */
    @ExceptionHandler({ MissingServletRequestPartException.class, MultipartException.class })
    public ResponseEntity<ErroResponseDTO> uploadInvalido(Exception ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Envie o arquivo como multipart/form-data no campo 'arquivo'", request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponseDTO> conflitoNoBanco(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violação de integridade em {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return resposta(HttpStatus.CONFLICT, "A ação conflita com dados já cadastrados", request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponseDTO> rotaInexistente(NoResourceFoundException ex, HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, "Rota não encontrada", request, null);
    }

    /** Stream de notificações (SSE) expirou: o app reconecta sozinho, sem corpo de erro. */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<Void> streamExpirado() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    /** O app fechou a conexão do stream no meio do envio: não há para quem responder. */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void conexaoEncerradaPeloCliente() {
        // nada a fazer
    }

    /** Qualquer outra coisa: registra no log e não vaza detalhes internos para o app. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponseDTO> erroInesperado(Exception ex, HttpServletRequest request) {
        // Exceções do próprio Spring MVC (405, 415, 406...) já sabem o status certo
        if (ex instanceof ErrorResponse erroDoSpring) {
            return resposta(erroDoSpring.getStatusCode(), "Requisição não suportada", request, null);
        }
        log.error("Erro inesperado em {} {}", request.getMethod(), request.getRequestURI(), ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro inesperado no servidor. Tente novamente.", request, null);
    }

    private ResponseEntity<ErroResponseDTO> resposta(
            HttpStatusCode status,
            String mensagem,
            HttpServletRequest request,
            Map<String, String> campos
    ) {
        HttpStatus httpStatus = HttpStatus.resolve(status.value());
        ErroResponseDTO corpo = new ErroResponseDTO(
                LocalDateTime.now(),
                status.value(),
                httpStatus != null ? httpStatus.getReasonPhrase() : String.valueOf(status.value()),
                mensagem,
                request.getRequestURI(),
                campos
        );
        return ResponseEntity.status(status).body(corpo);
    }
}
