package br.cefetmg.schoolsync_api.dto.notificacao;

import java.util.List;

public record NotificacoesPaginaDTO(List<NotificacaoResponseDTO> itens, boolean temMais, long naoLidas) {
}
