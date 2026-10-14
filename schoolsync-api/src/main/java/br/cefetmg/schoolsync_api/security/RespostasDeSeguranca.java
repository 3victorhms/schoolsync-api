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

// classe pra devolver os erros 401 e 403 do spring security no mesmo formato json do resto da api.
// esses erros acontecem no filtro, antes de chegar no controller, por isso o TratamentoGlobalDeErros n pega eles
@Component
@RequiredArgsConstructor
public class RespostasDeSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        escrever(request, response, HttpStatus.UNAUTHORIZED, "Faça login para acessar este recurso");
    }

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
