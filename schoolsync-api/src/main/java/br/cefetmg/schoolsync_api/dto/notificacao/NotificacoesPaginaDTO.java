package br.cefetmg.schoolsync_api.dto.notificacao;

import java.util.List;

/**
 * Feito com auxílio do Claude: página de notificações. {@code naoLidas} conta todas as
 * não lidas do usuário (não só as desta página), para o contador do sino continuar certo.
 */
public record NotificacoesPaginaDTO(List<NotificacaoResponseDTO> itens, boolean temMais, long naoLidas) {
}
