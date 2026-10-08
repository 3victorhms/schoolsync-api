package br.cefetmg.schoolsync_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 200, unique = true)
    private String email;

    @Column(nullable = false, length = 255)
    private String senha;

    @Column(columnDefinition = "TEXT")
    private String foto;

    // a conta é desativada e não excluída [RN]
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean ativo = true;

    /**
     * Perfil global (USUARIO ou ADMIN). O default no banco permite criar a coluna
     * com o ddl-auto mesmo com usuários já cadastrados: todos viram USUARIO.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'USUARIO'")
    private Perfil perfil = Perfil.USUARIO;
}
