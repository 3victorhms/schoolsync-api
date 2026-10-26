package br.cefetmg.schoolsync_api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.cefetmg.schoolsync_api.entity.Perfil;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;

// modo manutenção com a API inteira de pé: o comum recebe 503, o admin continua usando
@SpringBootTest
@AutoConfigureMockMvc
class ManutencaoTest {

    private static final String SENHA = "senhaDeTeste123";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private UsuarioRepository usuarioRepository;

    private MockHttpServletRequestBuilder comJson(MockHttpServletRequestBuilder pedido, Object corpo) throws Exception {
        return pedido.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo));
    }

    private String novoUsuarioLogado(String nome, boolean admin) throws Exception {
        String email = nome + "-" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";
        mvc.perform(comJson(post("/usuarios"), Map.of("nome", nome, "email", email, "senha", SENHA)))
                .andExpect(status().isCreated());
        if (admin) {
            usuarioRepository.findByEmail(email).ifPresent(usuario -> {
                usuario.setPerfil(Perfil.ADMIN);
                usuarioRepository.save(usuario);
            });
        }
        String resposta = mvc.perform(comJson(post("/usuarios/autenticar"), Map.of("email", email, "senha", SENHA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(resposta).get("token").asText();
    }

    @Test
    void soOAdminUsaOAppDuranteAManutencao() throws Exception {
        String admin = novoUsuarioLogado("admin", true);
        String comum = novoUsuarioLogado("comum", false);

        mvc.perform(get("/salas").header("Authorization", comum)).andExpect(status().isOk());

        mvc.perform(comJson(put("/admin/manutencao"), Map.of("ativa", true, "previsao", "hoje às 22h"))
                        .header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(true));
        try {
            mvc.perform(get("/salas").header("Authorization", comum))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.manutencao").value(true))
                    .andExpect(jsonPath("$.previsao").value("hoje às 22h"));
            // o admin continua usando normalmente
            mvc.perform(get("/salas").header("Authorization", admin)).andExpect(status().isOk());
            // o status é público, pra tela de manutenção saber quando voltou
            mvc.perform(get("/sistema/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ativa").value(true))
                    .andExpect(jsonPath("$.alteradoPor").doesNotExist());
            // o comum n consegue desligar
            mvc.perform(comJson(put("/admin/manutencao"), Map.of("ativa", false)).header("Authorization", comum))
                    .andExpect(status().isServiceUnavailable());
        } finally {
            // o contexto do spring é compartilhado com os outros testes: desliga antes de sair
            mvc.perform(comJson(put("/admin/manutencao"), Map.of("ativa", false)).header("Authorization", admin))
                    .andExpect(status().isOk());
        }

        mvc.perform(get("/salas").header("Authorization", comum)).andExpect(status().isOk());
    }
}
