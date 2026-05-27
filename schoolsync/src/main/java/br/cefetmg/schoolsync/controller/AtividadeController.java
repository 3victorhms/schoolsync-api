package br.cefetmg.schoolsync.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.cefetmg.schoolsync.model.Atividade;
import br.cefetmg.schoolsync.repository.AtividadeRepository;

@RestController
@RequestMapping("/api/v1/atividades")

public class AtividadeController {

    private AtividadeRepository repository;

    public AtividadeController(AtividadeRepository repository) {
        this.repository = repository;
    }

    @GetMapping("")
    public List<Atividade> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Atividade getById(@PathVariable Long id) {
        return repository.findById(id).orElse(null);
    }

    @PostMapping("")
    public Atividade inserir(@RequestBody Atividade atividade) {
        atividade.setId(null);
        repository.save(atividade);
        return atividade;
    }

    @PutMapping("")
    public Atividade alterarAtividade(@RequestBody Atividade ent) {

        if (ent.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "id é obrigatório");
        }

        repository.save(ent);
        return ent;
    }

    @DeleteMapping("/{id}")
    public Atividade excluirAtividade(@PathVariable Long id) {

        Atividade ent = repository.findById(id).orElse(null);

        if (ent == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Atividade com id: " + id + " não encontrada");
        }

        repository.deleteById(id);
        return ent;
    }

}
