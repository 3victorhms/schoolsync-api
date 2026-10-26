package br.cefetmg.schoolsync_api.dto.usuario;

import br.cefetmg.schoolsync_api.entity.Perfil;
import br.cefetmg.schoolsync_api.entity.Usuario;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDTO {

    private String id;
    private String nome;
    private String email;
    private String foto;
    private boolean ativo;
    private Perfil perfil;
    // a conta já entrou pelo google (o perfil usa pra explicar que dá pra criar uma senha pelo "esqueci minha senha")
    private boolean contaGoogle;

    public UsuarioResponseDTO(Usuario usuario) {
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.email = usuario.getEmail();
        this.foto = usuario.getFoto();
        this.ativo = usuario.isAtivo();
        this.perfil = usuario.getPerfil();
        this.contaGoogle = usuario.getGoogleId() != null;
    }
}
