package br.cefetmg.schoolsync_api.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_configuracao_sistema")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracaoSistema {

    @Id
    @Column(length = 20)
    private String id;

    @Column(nullable = false)
    private boolean manutencao = false;

    @Column(length = 300)
    private String mensagem;

    @Column(length = 60)
    private String previsao;

    @Column(name = "alterado_em")
    private LocalDateTime alteradoEm;

    @Column(name = "alterado_por", length = 150)
    private String alteradoPor;
}
