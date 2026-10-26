package br.cefetmg.schoolsync_api.security;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.cefetmg.schoolsync_api.dto.sistema.ManutencaoDTO;
import br.cefetmg.schoolsync_api.service.ManutencaoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// com o modo manutenção ligado, quem n é admin recebe 503 em tudo, menos no login (pro admin
// conseguir entrar) e no /sistema/status (que o app consulta pra saber quando voltou).
// roda depois do filtro do JWT, ent já sabe se quem chamou é admin.
// n é @Component de propósito: só existe dentro da cadeia do spring security (SecurityConfig)
public class ManutencaoFilter extends OncePerRequestFilter {

    private final ManutencaoService manutencaoService;
    private final ObjectMapper objectMapper;

    public ManutencaoFilter(ManutencaoService manutencaoService, ObjectMapper objectMapper) {
        this.manutencaoService = manutencaoService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (liberado(request) || ehAdmin() || !manutencaoService.ativa()) {
            filterChain.doFilter(request, response);
            return;
        }

        ManutencaoDTO estado = manutencaoService.estado();
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("timestamp", LocalDateTime.now().toString());
        corpo.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        corpo.put("error", HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase());
        corpo.put("message", estado.mensagem());
        corpo.put("path", request.getRequestURI());
        // o app olha esse campo pra diferenciar a manutenção de um 503 qualquer do servidor
        corpo.put("manutencao", true);
        if (estado.previsao() != null) {
            corpo.put("previsao", estado.previsao());
        }

        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setHeader("Retry-After", "300");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), corpo);
    }

    private boolean liberado(HttpServletRequest request) {
        String caminho = request.getRequestURI();
        return "OPTIONS".equals(request.getMethod())
                || caminho.startsWith("/sistema/")
                || caminho.equals("/usuarios/autenticar")
                || caminho.equals("/usuarios/google")
                || caminho.startsWith("/swagger-ui")
                || caminho.startsWith("/v3/api-docs");
    }

    private boolean ehAdmin() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao != null && autenticacao.getAuthorities().stream()
                .anyMatch(permissao -> "ROLE_ADMIN".equals(permissao.getAuthority()));
    }
}
