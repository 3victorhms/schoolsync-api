package br.cefetmg.schoolsync_api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.cefetmg.schoolsync_api.dto.nota.NotaRequestDTO;
import br.cefetmg.schoolsync_api.dto.nota.NotaResponseDTO;
import br.cefetmg.schoolsync_api.entity.Atividade;
import br.cefetmg.schoolsync_api.entity.Caderno;
import br.cefetmg.schoolsync_api.entity.Materia;
import br.cefetmg.schoolsync_api.entity.Nota;
import br.cefetmg.schoolsync_api.entity.Periodo;
import br.cefetmg.schoolsync_api.entity.Sala;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.AtividadeRepository;
import br.cefetmg.schoolsync_api.repository.CadernoRepository;
import br.cefetmg.schoolsync_api.repository.MembrosRepository;
import br.cefetmg.schoolsync_api.repository.NotaRepository;
import br.cefetmg.schoolsync_api.repository.SalaRepository;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotaService {

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final double MARGEM = 0.000001d;

    private final NotaRepository notaRepository;
    private final SalaRepository salaRepository;
    private final AtividadeRepository atividadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final MembrosRepository membrosRepository;
    private final CadernoRepository cadernoRepository;

    @Transactional(readOnly = true)
    public List<NotaResponseDTO> listarMinhasDaSala(String idSala, String idUsuario) {
        validarMembro(idSala, idUsuario);
        return notaRepository.findBySala_IdAndUsuario_IdOrderByDataAsc(idSala, idUsuario).stream()
                .map(NotaResponseDTO::new)
                .toList();
    }

    @Transactional
    public NotaResponseDTO lancar(String idSala, NotaRequestDTO dto, String idUsuario) {
        validarMembro(idSala, idUsuario);
        Sala sala = salaRepository.findById(idSala)
                .orElseThrow(() -> new EntityNotFoundException("Sala não encontrada"));
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        Nota nota = new Nota();
        nota.setSala(sala);
        nota.setUsuario(usuario);
        nota.setDataRegistro(LocalDateTime.now());

        if (temTexto(dto.getIdAtividade())) {
            Atividade atividade = atividadeRepository.findById(dto.getIdAtividade())
                    .orElseThrow(() -> new EntityNotFoundException("Atividade não encontrada"));
            if (!atividade.getSala().getId().equals(idSala)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A atividade não é desta sala");
            }
            if (notaRepository.findByAtividade_IdAndUsuario_Id(atividade.getId(), idUsuario).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Você já lançou a nota desta atividade. Edite a nota existente.");
            }
            nota.setAtividade(atividade);
            copiarDaAtividade(nota, atividade);
        } else {
            preencherAvulsa(nota, sala, dto, idUsuario, null);
        }

        nota.setValorObtido(validarValorObtido(dto.getValorObtido(), nota.getValorMaximo()));
        Nota salva = notaRepository.save(nota);

        if (salva.getAtividade() != null) {
            concluirNoCaderno(salva.getAtividade(), usuario);
        }
        return new NotaResponseDTO(salva);
    }

    // quem recebeu nota já entregou a atividade, entao ela fica concluída no caderno do aluno
    // se ela ainda n estava no caderno, entra já concluída (assim o calendario, as arquivadas e o progresso batem com o boletim)
    private void concluirNoCaderno(Atividade atividade, Usuario usuario) {
        Caderno caderno = cadernoRepository.findByAtividade_IdAndUsuario_Id(atividade.getId(), usuario.getId())
                .orElseGet(() -> {
                    Caderno novo = new Caderno();
                    novo.setAtividade(atividade);
                    novo.setUsuario(usuario);
                    novo.setDataAdicionado(LocalDateTime.now());
                    return novo;
                });
        caderno.setStatus("concluido");
        cadernoRepository.save(caderno);
    }

    @Transactional
    public NotaResponseDTO atualizar(String idNota, NotaRequestDTO dto, String idUsuario) {
        Nota nota = buscarMinha(idNota, idUsuario);

        if (nota.isAvulsa()) {
            preencherAvulsa(nota, nota.getSala(), dto, idUsuario, nota.getId());
        } else {
            copiarDaAtividade(nota, nota.getAtividade());
        }

        nota.setValorObtido(validarValorObtido(dto.getValorObtido(), nota.getValorMaximo()));
        return new NotaResponseDTO(notaRepository.save(nota));
    }

    @Transactional
    public void excluir(String idNota, String idUsuario) {
        notaRepository.delete(buscarMinha(idNota, idUsuario));
    }

    // se a atividade mudar (matéria, data ou valor) as notas ligadas a ela acompanham
    // se ela deixar de valer ponto, as notas q já foram lançadas viram avulsas
    @Transactional
    public void sincronizarComAtividade(Atividade atividade) {
        if (atividade.getValor() == null || atividade.getValor() <= 0) {
            desvincularDaAtividade(atividade);
            return;
        }
        for (Nota nota : notaRepository.findByAtividade_Id(atividade.getId())) {
            copiarDaAtividade(nota, atividade);
            nota.setValorObtido(Math.min(nota.getValorObtido(), nota.getValorMaximo()));
        }
    }

    // quando a atividade é excluída as notas q os alunos já lançaram n somem, viram avulsas
    // e o título da atividade vira a descrição da nota
    @Transactional
    public void desvincularDaAtividade(Atividade atividade) {
        for (Nota nota : notaRepository.findByAtividade_Id(atividade.getId())) {
            nota.setDescricao(limitar(atividade.getTitulo(), 150));
            nota.setAtividade(null);
        }
        notaRepository.flush();
    }

    private void copiarDaAtividade(Nota nota, Atividade atividade) {
        if (atividade.getValor() == null || atividade.getValor() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Esta atividade não vale ponto, então não entra no boletim");
        }
        nota.setMateria(atividade.getMateria());
        nota.setPeriodo(atividade.getPeriodo());
        nota.setValorMaximo(atividade.getValor());
        nota.setData(atividade.getDataEntrega());
    }

    private void preencherAvulsa(Nota nota, Sala sala, NotaRequestDTO dto, String idUsuario, String idIgnorado) {
        if (!temTexto(dto.getDescricao())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Descreva a avaliação (ex.: Prova de março)");
        }
        if (dto.getValorMaximo() == null || dto.getValorMaximo() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe quanto a avaliação valia");
        }
        if (dto.getData() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a data da avaliação");
        }
        if (dto.getData().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nota avulsa é para avaliações que já aconteceram: a data não pode ser no futuro");
        }

        Materia materia = sala.getMaterias().stream()
                .filter(m -> m.getId().equals(dto.getIdMateria()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Escolha uma das matérias da sala"));

        Periodo periodo = sala.periodoDaData(dto.getData())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "A data " + dto.getData().format(DATA_BR) + " está fora dos períodos letivos da sala"));

        double usadoPelaSala = atividadeRepository.findByMateria_IdAndPeriodo_Id(materia.getId(), periodo.getId()).stream()
                .mapToDouble(Atividade::getValor)
                .sum();
        double usadoPorAvulsas = notaRepository
                .findByUsuario_IdAndMateria_IdAndPeriodo_IdAndAtividadeIsNull(idUsuario, materia.getId(), periodo.getId())
                .stream()
                .filter(outra -> idIgnorado == null || !outra.getId().equals(idIgnorado))
                .mapToDouble(Nota::getValorMaximo)
                .sum();
        double disponivel = Math.max(0, periodo.getPontuacaoMaxima() - usadoPelaSala - usadoPorAvulsas);

        if (dto.getValorMaximo() > disponivel + MARGEM) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    String.format("Em %s no %s só restam %.2f pontos (máximo do período: %.2f)",
                            materia.getNome(), periodo.getNome(), disponivel, periodo.getPontuacaoMaxima()));
        }

        nota.setDescricao(dto.getDescricao().trim());
        nota.setMateria(materia);
        nota.setPeriodo(periodo);
        nota.setValorMaximo(dto.getValorMaximo());
        nota.setData(dto.getData());
    }

    private double validarValorObtido(Double valorObtido, Double valorMaximo) {
        if (valorObtido == null || valorObtido < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A nota não pode ser negativa");
        }
        if (valorObtido > valorMaximo + MARGEM) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("A nota não pode passar do valor da avaliação (%.2f)", valorMaximo));
        }
        return valorObtido;
    }

    private Nota buscarMinha(String idNota, String idUsuario) {
        Nota nota = notaRepository.findById(idNota)
                .orElseThrow(() -> new EntityNotFoundException("Nota não encontrada"));
        if (!nota.getUsuario().getId().equals(idUsuario)) {
            throw new EntityNotFoundException("Nota não encontrada");
        }
        return nota;
    }

    private void validarMembro(String idSala, String idUsuario) {
        if (!membrosRepository.existsBySala_IdAndUsuario_Id(idSala, idUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não participa desta sala");
        }
    }

    private static boolean temTexto(String texto) {
        return texto != null && !texto.isBlank();
    }

    private static String limitar(String texto, int tamanho) {
        return texto == null || texto.length() <= tamanho ? texto : texto.substring(0, tamanho);
    }
}
