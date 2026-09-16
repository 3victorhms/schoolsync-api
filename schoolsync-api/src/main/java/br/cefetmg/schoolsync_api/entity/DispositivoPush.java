package br.cefetmg.schoolsync_api.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// classe pra armazenar os tokens dos dispostivos para enviar as notificações
@Entity
@Table(name = "tb_dispositivo_push", uniqueConstraints = @UniqueConstraint(name = "uk_dispositivo_push_token", columnNames = "token"), indexes = @Index(name = "idx_dispositivo_push_usuario", columnList = "usuario_id"))
@Getter
@Setter
@NoArgsConstructor
public class DispositivoPush {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 512)
    private String token;

    @Column(nullable = false, length = 20)
    private String plataforma;

    @Column(nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    @PreUpdate
    void atualizarHorario() {
        atualizadoEm = LocalDateTime.now();
    }
}
