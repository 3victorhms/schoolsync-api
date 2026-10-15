package br.cefetmg.schoolsync_api.dto.usuario;

import br.cefetmg.schoolsync_api.entity.Usuario;

// dados de um usuário q qualquer pessoa logada pode ver (ex: nome de quem criou a atividade).
// e-mail, perfil e situação da conta ficam de fora
public record UsuarioPublicoDTO(String id, String nome, String foto) {

    public UsuarioPublicoDTO(Usuario usuario) {
        this(usuario.getId(), usuario.getNome(), usuario.getFoto());
    }
}
