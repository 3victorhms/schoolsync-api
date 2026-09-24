package br.cefetmg.schoolsync_api.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        String token = null;

        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        } else if (ehStreamDeNotificacoes(request)) {
            // O EventSource do navegador não envia cabeçalhos, então o stream
            // de notificações recebe o token pela URL (?token=...).
            token = request.getParameter("token");
        }

        if (token != null && !token.isBlank()) {

            if (jwtService.tokenValido(token)) {
                String idUsuario = jwtService.extrairIdUsuario(token);
                usuarioRepository.findById(idUsuario).ifPresent(this::autenticar);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean ehStreamDeNotificacoes(HttpServletRequest request) {
        return "GET".equals(request.getMethod())
                && request.getRequestURI().matches(".*/notificacoes/usuario/[^/]+/stream$");
    }

    private void autenticar(Usuario usuario) {
        if (!usuario.isAtivo()) {
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                usuario,
                null,
                List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
