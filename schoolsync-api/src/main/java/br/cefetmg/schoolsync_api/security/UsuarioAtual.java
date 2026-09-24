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
 * Os endpoints ainda recebem o id do usuário (idUsuario, idUsuarioLogado, idCriador...)
 * para manter compatível o app já publicado, mas esse valor não é mais confiável:
 * ele precisa ser igual ao do token, senão a requisição é recusada com 403.
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

    /** Garante que o id enviado pelo app é o do próprio usuário logado. */
    public void validar(String idInformado) {
        if (idInformado == null || !idInformado.equals(id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você não tem permissão para realizar esta ação em nome de outro usuário");
        }
    }
}
