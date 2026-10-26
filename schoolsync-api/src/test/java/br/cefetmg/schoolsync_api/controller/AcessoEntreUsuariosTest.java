package br.cefetmg.schoolsync_api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Feito com auxílio do Claude: testes de controller (MockMvc) com a API inteira de pé
 * (segurança, filtro do JWT, controllers e banco H2 dos testes).
 *
 * Provam a correção do IDOR: o usuário vem do token, então ninguém lê nem altera a sala
 * ou as notas de outra pessoa trocando um id na URL.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AcessoEntreUsuariosTest {

    private static final String SENHA = "senhaDeTeste123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    // ===================== auxiliares =====================

    private String emailUnico(String nome) {
        return nome + "-" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";
    }

    private MockHttpServletRequestBuilder comJson(MockHttpServletRequestBuilder pedido, Object corpo) throws Exception {
        return pedido.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo));
    }

    private MockHttpServletRequestBuilder comToken(MockHttpServletRequestBuilder pedido, String token) {
        return pedido.header("Authorization", "Bearer " + token);
    }

    private JsonNode lerJson(String conteudo) throws Exception {
        return json.readTree(conteudo);
    }

    private void cadastrar(String nome, String email) throws Exception {
        mvc.perform(comJson(post("/usuarios"), Map.of("nome", nome, "email", email, "senha", SENHA)))
                .andExpect(status().isCreated());
    }

    private String logar(String email) throws Exception {
        String resposta = mvc.perform(comJson(post("/usuarios/autenticar"), Map.of("email", email, "senha", SENHA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return lerJson(resposta).get("token").asText();
    }

    private String novoUsuarioLogado(String nome) throws Exception {
        String email = emailUnico(nome);
        cadastrar(nome, email);
        return logar(email);
    }

    /** Sala com 1 matéria e 2 semestres; o 1º semestre contém hoje. */
    private JsonNode criarSala(String token) throws Exception {
        LocalDate hoje = LocalDate.now();
        Map<String, Object> corpo = Map.of(
                "nome", "Sala de teste",
                "materias", List.of(Map.of("nome", "Matemática")),
                "tipoPeriodo", "SEMESTRE",
                "periodos", List.of(
                        Map.of("dataInicio", hoje.minusMonths(2).toString(), "dataFim", hoje.plusDays(10).toString(), "pontuacaoMaxima", 50),
                        Map.of("dataInicio", hoje.plusDays(11).toString(), "dataFim", hoje.plusMonths(6).toString(), "pontuacaoMaxima", 50)));
        String resposta = mvc.perform(comToken(comJson(post("/salas"), corpo), token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return lerJson(resposta);
    }

    private String lancarAvulsa(String token, JsonNode sala) throws Exception {
        Map<String, Object> corpo = Map.of(
                "descricao", "Prova antiga",
                "idMateria", sala.get("materias").get(0).get("id").asText(),
                "valorMaximo", 10,
                "valorObtido", 8,
                "data", LocalDate.now().toString());
        String resposta = mvc.perform(comToken(comJson(post("/salas/" + sala.get("id").asText() + "/notas"), corpo), token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return lerJson(resposta).get("id").asText();
    }

    // ===================== testes =====================

    @Test
    void semTokenNaoAcessaNada() throws Exception {
        mvc.perform(get("/salas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/notificacoes")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenFalsoEhRecusado() throws Exception {
        mvc.perform(comToken(get("/salas"), "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJxdWFscXVlciJ9.assinatura-falsa"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void quemNaoEhDaSalaNaoVeASalaNemAsNotas() throws Exception {
        String tokenLider = novoUsuarioLogado("lider");
        String tokenIntruso = novoUsuarioLogado("intruso");
        JsonNode sala = criarSala(tokenLider);
        String idSala = sala.get("id").asText();

        mvc.perform(comToken(get("/salas/" + idSala), tokenLider)).andExpect(status().isOk());

        mvc.perform(comToken(get("/salas/" + idSala), tokenIntruso)).andExpect(status().isForbidden());
        mvc.perform(comToken(get("/salas/" + idSala + "/notas"), tokenIntruso)).andExpect(status().isForbidden());
        mvc.perform(comToken(comJson(post("/salas/" + idSala + "/notas"), Map.of(
                        "descricao", "Tentativa", "idMateria", sala.get("materias").get(0).get("id").asText(),
                        "valorMaximo", 5, "valorObtido", 5, "data", LocalDate.now().toString())), tokenIntruso))
                .andExpect(status().isForbidden());
        // a sala do líder não aparece na lista de salas do intruso
        mvc.perform(comToken(get("/salas"), tokenIntruso))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void colegaDeSalaNaoVeNemMexeNaNotaDoOutro() throws Exception {
        String tokenLider = novoUsuarioLogado("lider");
        String tokenColega = novoUsuarioLogado("colega");
        JsonNode sala = criarSala(tokenLider);
        String idSala = sala.get("id").asText();
        String idNotaDoLider = lancarAvulsa(tokenLider, sala);

        mvc.perform(comToken(post("/salas/entrar").param("codigoConvite", sala.get("codigoConvite").asText()), tokenColega))
                .andExpect(status().isOk());
        mvc.perform(comToken(get("/salas/" + idSala), tokenColega)).andExpect(status().isOk());

        // o boletim é pessoal: o colega vê a lista dele, vazia
        mvc.perform(comToken(get("/salas/" + idSala + "/notas"), tokenColega))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // e não consegue editar nem apagar a nota do líder: para ele, ela não existe
        mvc.perform(comToken(comJson(put("/notas/" + idNotaDoLider), Map.of(
                        "descricao", "Alterada", "idMateria", sala.get("materias").get(0).get("id").asText(),
                        "valorMaximo", 10, "valorObtido", 0, "data", LocalDate.now().toString())), tokenColega))
                .andExpect(status().isNotFound());
        mvc.perform(comToken(delete("/notas/" + idNotaDoLider), tokenColega)).andExpect(status().isNotFound());

        // a nota do líder continua lá, intacta
        mvc.perform(comToken(get("/salas/" + idSala + "/notas"), tokenLider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].valorObtido").value(8.0));
    }

    @Test
    void colegaNaoEditaNemExcluiASalaDoLider() throws Exception {
        String tokenLider = novoUsuarioLogado("lider");
        String tokenColega = novoUsuarioLogado("colega");
        JsonNode sala = criarSala(tokenLider);
        String idSala = sala.get("id").asText();
        mvc.perform(comToken(post("/salas/entrar").param("codigoConvite", sala.get("codigoConvite").asText()), tokenColega))
                .andExpect(status().isOk());

        mvc.perform(comToken(delete("/salas/" + idSala), tokenColega)).andExpect(status().isForbidden());
        mvc.perform(comToken(get("/salas/" + idSala), tokenLider)).andExpect(status().isOk());
    }

    @Test
    void muitasSenhasErradasBloqueiamOLoginCom429() throws Exception {
        String email = emailUnico("bruta");
        cadastrar("bruta", email);

        for (int i = 0; i < 5; i++) {
            mvc.perform(comJson(post("/usuarios/autenticar"), Map.of("email", email, "senha", "senhaErrada" + i)))
                    .andExpect(status().isUnauthorized());
        }
        // bloqueado: nem a senha certa entra até o bloqueio acabar
        mvc.perform(comJson(post("/usuarios/autenticar"), Map.of("email", email, "senha", SENHA)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Muitas tentativas")));
    }

    @Test
    void notificacoesVemPaginadas() throws Exception {
        String token = novoUsuarioLogado("paginado");
        mvc.perform(comToken(get("/notificacoes").param("tamanho", "5"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isArray())
                .andExpect(jsonPath("$.temMais").value(false))
                .andExpect(jsonPath("$.naoLidas").value(0));
        mvc.perform(comToken(get("/notificacoes").param("antesDe", "ontem"), token))
                .andExpect(status().isBadRequest());
    }
}
