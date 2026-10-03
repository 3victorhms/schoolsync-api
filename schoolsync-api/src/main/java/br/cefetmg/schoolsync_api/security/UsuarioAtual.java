package br.cefetmg.schoolsync_api.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import br.cefetmg.schoolsync_api.entity.Usuario;

/**
 * Usuário autenticado pelo token JWT da requisição.
 *
 * v2: os endpoints não recebem mais o id do usuário (idUsuario, idUsuarioLogado,
 * idCriador...). Quem está fazendo a ação é sempre o dono do token.
 */
@Component
public class UsuarioAtual {

    public Usuario usuario() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (principal instanceof Usuario usuario) {
            return usuario;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
    }

    public String id() {
        return usuario().getId();
    }
}
