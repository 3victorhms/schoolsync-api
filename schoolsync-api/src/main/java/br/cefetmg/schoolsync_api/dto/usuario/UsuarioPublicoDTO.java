package br.cefetmg.schoolsync_api.dto.usuario;

import br.cefetmg.schoolsync_api.entity.Usuario;

public record UsuarioPublicoDTO(String id, String nome, String foto) {

    public UsuarioPublicoDTO(Usuario usuario) {
        this(usuario.getId(), usuario.getNome(), usuario.getFoto());
    }
}
