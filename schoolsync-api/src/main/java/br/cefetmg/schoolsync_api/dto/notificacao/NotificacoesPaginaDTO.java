package br.cefetmg.schoolsync_api.dto.notificacao;

import java.util.List;

// página de notificações. naoLidas conta todas as não lidas do usuário (n só as da página)
// pro contador do sino ficar certo
public record NotificacoesPaginaDTO(List<NotificacaoResponseDTO> itens, boolean temMais, long naoLidas) {
}
