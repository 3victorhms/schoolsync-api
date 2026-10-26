package br.cefetmg.schoolsync_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import br.cefetmg.schoolsync_api.config.RecuperacaoSenhaProperties;
import br.cefetmg.schoolsync_api.entity.CodigoRecuperacaoSenha;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.CodigoRecuperacaoSenhaRepository;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class RecuperacaoSenhaServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CodigoRecuperacaoSenhaRepository codigoRepository;
    @Mock private EmailService emailService;

    // BCrypt de verdade (com custo baixo pra ser rápido): o teste confere o hash como a API confere
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final RecuperacaoSenhaProperties properties =
            new RecuperacaoSenhaProperties("chave", "nao-responda@schoolsync.app", "SchoolSync", 15);

    private RecuperacaoSenhaService service;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        service = new RecuperacaoSenhaService(usuarioRepository, codigoRepository, passwordEncoder, emailService, properties);
        usuario = new Usuario();
        usuario.setId("u1");
        usuario.setNome("Marcos Daniel");
        usuario.setEmail("marcos@exemplo.com");
        usuario.setSenha(passwordEncoder.encode("senhaAntiga1"));
        usuario.setAtivo(true);
    }

    private CodigoRecuperacaoSenha codigo(String valor, LocalDateTime criadoEm, LocalDateTime expiraEm, int tentativas) {
        CodigoRecuperacaoSenha registro = new CodigoRecuperacaoSenha();
        registro.setUsuario(usuario);
        registro.setCodigoHash(passwordEncoder.encode(valor));
        registro.setCriadoEm(criadoEm);
        registro.setExpiraEm(expiraEm);
        registro.setTentativas(tentativas);
        return registro;
    }

    @Test
    void semEmailConfiguradoResponde503ENaoProcuraOUsuario() {
        when(emailService.configurado()).thenReturn(false);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.solicitar("marcos@exemplo.com"));

        assertEquals(503, erro.getStatusCode().value());
        verify(usuarioRepository, never()).findByEmail(anyString());
    }

    @Test
    void emailSemContaNaoEnviaNadaMasNaoDaErro() {
        when(emailService.configurado()).thenReturn(true);
        when(usuarioRepository.findByEmail("ninguem@exemplo.com")).thenReturn(Optional.empty());

        service.solicitar("ninguem@exemplo.com");

        verify(codigoRepository, never()).save(any());
        verify(emailService, never()).enviar(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void geraCodigoDeSeisDigitosGuardaSoOHashEEnviaPorEmail() {
        when(emailService.configurado()).thenReturn(true);
        when(usuarioRepository.findByEmail("marcos@exemplo.com")).thenReturn(Optional.of(usuario));
        when(codigoRepository.findFirstByUsuarioOrderByCriadoEmDesc(usuario)).thenReturn(Optional.empty());

        service.solicitar("marcos@exemplo.com");

        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviar(eq("marcos@exemplo.com"), eq("Marcos Daniel"), anyString(), anyString(), texto.capture());
        Matcher m = Pattern.compile("\\b(\\d{6})\\b").matcher(texto.getValue());
        assertTrue(m.find(), "o e-mail deve trazer o código");
        String codigoEnviado = m.group(1);

        ArgumentCaptor<CodigoRecuperacaoSenha> salvo = ArgumentCaptor.forClass(CodigoRecuperacaoSenha.class);
        verify(codigoRepository).save(salvo.capture());
        assertTrue(!salvo.getValue().getCodigoHash().contains(codigoEnviado), "o código n pode ficar em texto puro");
        assertTrue(passwordEncoder.matches(codigoEnviado, salvo.getValue().getCodigoHash()));
        assertTrue(salvo.getValue().getExpiraEm().isAfter(LocalDateTime.now().plusMinutes(14)));
        verify(codigoRepository).deleteByUsuario(usuario);
    }

    @Test
    void pedidoRepetidoEmMenosDeUmMinutoNaoMandaOutroEmail() {
        when(emailService.configurado()).thenReturn(true);
        when(usuarioRepository.findByEmail("marcos@exemplo.com")).thenReturn(Optional.of(usuario));
        LocalDateTime agora = LocalDateTime.now();
        when(codigoRepository.findFirstByUsuarioOrderByCriadoEmDesc(usuario))
                .thenReturn(Optional.of(codigo("123456", agora.minusSeconds(20), agora.plusMinutes(14), 0)));

        service.solicitar("marcos@exemplo.com");

        verify(emailService, never()).enviar(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(codigoRepository, never()).save(any());
    }

    @Test
    void codigoCertoTrocaASenhaDerrubaAsSessoesEApagaOCodigo() {
        LocalDateTime agora = LocalDateTime.now();
        CodigoRecuperacaoSenha registro = codigo("654321", agora.minusMinutes(2), agora.plusMinutes(13), 0);
        when(usuarioRepository.findByEmail("marcos@exemplo.com")).thenReturn(Optional.of(usuario));
        when(codigoRepository.findFirstByUsuarioOrderByCriadoEmDesc(usuario)).thenReturn(Optional.of(registro));

        service.redefinir("marcos@exemplo.com", "654321", "senhaNova123");

        assertTrue(passwordEncoder.matches("senhaNova123", usuario.getSenha()));
        assertNotNull(usuario.getCredenciaisAlteradasEm());
        verify(usuarioRepository).save(usuario);
        verify(codigoRepository).delete(registro);
    }

    @Test
    void codigoErradoContaATentativaENaoTrocaASenha() {
        LocalDateTime agora = LocalDateTime.now();
        CodigoRecuperacaoSenha registro = codigo("654321", agora.minusMinutes(2), agora.plusMinutes(13), 0);
        String senhaAntes = usuario.getSenha();
        when(usuarioRepository.findByEmail("marcos@exemplo.com")).thenReturn(Optional.of(usuario));
        when(codigoRepository.findFirstByUsuarioOrderByCriadoEmDesc(usuario)).thenReturn(Optional.of(registro));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.redefinir("marcos@exemplo.com", "000000", "senhaNova123"));

        assertEquals(400, erro.getStatusCode().value());
        assertEquals(1, registro.getTentativas());
        assertEquals(senhaAntes, usuario.getSenha());
        verify(codigoRepository).save(registro);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void quintaTentativaErradaDescartaOCodigo() {
        LocalDateTime agora = LocalDateTime.now();
        CodigoRecuperacaoSenha registro = codigo("654321", agora.minusMinutes(2), agora.plusMinutes(13),
                RecuperacaoSenhaService.MAX_TENTATIVAS - 1);
        when(usuarioRepository.findByEmail("marcos@exemplo.com")).thenReturn(Optional.of(usuario));
        when(codigoRepository.findFirstByUsuarioOrderByCriadoEmDesc(usuario)).thenReturn(Optional.of(registro));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.redefinir("marcos@exemplo.com", "111111", "senhaNova123"));

        assertTrue(erro.getReason().contains("Muitas tentativas"));
        verify(codigoRepository).delete(registro);
    }

    @Test
    void codigoExpiradoNaoVale() {
        LocalDateTime agora = LocalDateTime.now();
        CodigoRecuperacaoSenha registro = codigo("654321", agora.minusMinutes(30), agora.minusMinutes(15), 0);
        when(usuarioRepository.findByEmail("marcos@exemplo.com")).thenReturn(Optional.of(usuario));
        when(codigoRepository.findFirstByUsuarioOrderByCriadoEmDesc(usuario)).thenReturn(Optional.of(registro));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.redefinir("marcos@exemplo.com", "654321", "senhaNova123"));

        assertEquals(400, erro.getStatusCode().value());
        verify(codigoRepository).delete(registro);
        verify(usuarioRepository, never()).save(any());
    }
}
