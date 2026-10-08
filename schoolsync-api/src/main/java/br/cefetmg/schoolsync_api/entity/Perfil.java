package br.cefetmg.schoolsync_api.entity;

/**
 * Perfil global do usuário no sistema (Spring Security: ROLE_USUARIO / ROLE_ADMIN).
 *
 * É diferente de "líder de sala" e "líder de grupo", que são permissões dentro de
 * uma sala ou grupo específico e continuam sendo conferidas nos services.
 * Não existe rota para virar ADMIN: o perfil é definido direto no banco.
 */
public enum Perfil {
    USUARIO,
    ADMIN
}
