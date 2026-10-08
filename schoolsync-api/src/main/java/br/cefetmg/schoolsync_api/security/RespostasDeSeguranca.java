package br.cefetmg.schoolsync_api.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.cefetmg.schoolsync_api.exception.ErroResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Respostas do Spring Security no mesmo formato de erro do resto da API.
 *
 * Esses erros acontecem no filtro, antes de chegar ao controller, por isso o
 * TratamentoGlobalDeErros (@RestControllerAdvice) não os alcança.
 */
@Component
@RequiredArgsConstructor
public class RespostasDeSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /** Rota protegida acessada sem token, ou com token inválido/expirado. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        escrever(request, response, HttpStatus.UNAUTHORIZED, "Faça login para acessar este recurso");
    }

    /** Usuário logado, mas sem o perfil exigido (ex.: USUARIO acessando /admin/**). */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        escrever(request, response, HttpStatus.FORBIDDEN, "Você não tem permissão para acessar este recurso");
    }

    private void escrever(HttpServletRequest request, HttpServletResponse response,
            HttpStatus status, String mensagem) throws IOException {
        ErroResponseDTO corpo = new ErroResponseDTO(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                request.getRequestURI(),
                null
        );
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), corpo);
    }
}
