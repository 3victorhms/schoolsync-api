package br.cefetmg.schoolsync_api.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.cefetmg.schoolsync_api.config.RecuperacaoSenhaProperties;
import br.cefetmg.schoolsync_api.entity.CodigoRecuperacaoSenha;
import br.cefetmg.schoolsync_api.entity.Usuario;
import br.cefetmg.schoolsync_api.repository.CodigoRecuperacaoSenhaRepository;
import br.cefetmg.schoolsync_api.repository.UsuarioRepository;

/**
 * Feito com auxílio do Claude: recuperação de senha por código enviado no e-mail.
 *
 * Regras:
 * - a resposta de "esqueci a senha" é a mesma se o e-mail existe ou não, para n revelar
 *   quem tem conta;
 * - o código tem 6 dígitos, vale {@code recuperacao-senha.validade-minutos} (15 por padrão)
 *   e só o hash fica no banco;
 * - pedir de novo antes de 60 s n gera outro e-mail (evita spam na caixa de alguém);
 * - 5 tentativas erradas descartam o código;
 * - ao redefinir, as sessões abertas caem (credenciaisAlteradasEm, igual à troca de senha no perfil).
 */
@Service
public class RecuperacaoSenhaService {

    static final int MAX_TENTATIVAS = 5;
    static final int INTERVALO_REENVIO_SEGUNDOS = 60;
    private static final String CODIGO_INVALIDO = "Código inválido ou expirado. Peça um novo código";

    private final Logger log = LoggerFactory.getLogger(RecuperacaoSenhaService.class);
    private final SecureRandom random = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final CodigoRecuperacaoSenhaRepository codigoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RecuperacaoSenhaProperties properties;

    public RecuperacaoSenhaService(UsuarioRepository usuarioRepository,
            CodigoRecuperacaoSenhaRepository codigoRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            RecuperacaoSenhaProperties properties) {
        this.usuarioRepository = usuarioRepository;
        this.codigoRepository = codigoRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.properties = properties;
    }

    @Transactional
    public void solicitar(String email) {
        if (!emailService.configurado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "A recuperação de senha não está disponível no momento");
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email.trim());
        if (usuarioOpt.isEmpty() || !usuarioOpt.get().isAtivo()) {
            log.debug("Recuperação pedida para um e-mail sem conta ativa");
            return;
        }
        Usuario usuario = usuarioOpt.get();
        LocalDateTime agora = LocalDateTime.now();

        Optional<CodigoRecuperacaoSenha> anterior = codigoRepository.findFirstByUsuarioOrderByCriadoEmDesc(usuario);
        if (anterior.isPresent() && anterior.get().getCriadoEm().isAfter(agora.minusSeconds(INTERVALO_REENVIO_SEGUNDOS))) {
            return;
        }

        String codigo = String.format("%06d", random.nextInt(1_000_000));

        codigoRepository.deleteByUsuario(usuario);
        CodigoRecuperacaoSenha novo = new CodigoRecuperacaoSenha();
        novo.setUsuario(usuario);
        novo.setCodigoHash(passwordEncoder.encode(codigo));
        novo.setCriadoEm(agora);
        novo.setExpiraEm(agora.plusMinutes(properties.validade()));
        codigoRepository.save(novo);

        emailService.enviar(usuario.getEmail(), usuario.getNome(),
                "Seu código para redefinir a senha do SchoolSync",
                html(usuario.getNome(), codigo),
                texto(usuario.getNome(), codigo));
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void redefinir(String email, String codigo, String novaSenha) {
        Usuario usuario = usuarioRepository.findByEmail(email.trim())
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, CODIGO_INVALIDO));

        CodigoRecuperacaoSenha registro = codigoRepository.findFirstByUsuarioOrderByCriadoEmDesc(usuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, CODIGO_INVALIDO));

        if (registro.getExpiraEm().isBefore(LocalDateTime.now())) {
            codigoRepository.delete(registro);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, CODIGO_INVALIDO);
        }

        if (!passwordEncoder.matches(codigo.trim(), registro.getCodigoHash())) {
            registro.setTentativas(registro.getTentativas() + 1);
            if (registro.getTentativas() >= MAX_TENTATIVAS) {
                codigoRepository.delete(registro);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Muitas tentativas erradas. Peça um novo código");
            }
            codigoRepository.save(registro);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código incorreto");
        }

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        // derruba as sessões abertas em outros aparelhos (ver JwtAuthenticationFilter)
        usuario.setCredenciaisAlteradasEm(LocalDateTime.now());
        usuarioRepository.save(usuario);
        codigoRepository.delete(registro);
    }

    private String primeiroNome(String nome) {
        if (nome == null || nome.isBlank()) return "";
        return nome.trim().split("\\s+")[0];
    }

    private String texto(String nome, String codigo) {
        return "Olá, " + primeiroNome(nome) + "!\n\n"
                + "Seu código para redefinir a senha do SchoolSync é: " + codigo + "\n\n"
                + "Ele vale por " + properties.validade() + " minutos. "
                + "Se você não pediu para trocar a senha, ignore este e-mail: sua senha continua a mesma.";
    }

    private String html(String nome, String codigo) {
        String nomeSeguro = org.springframework.web.util.HtmlUtils.htmlEscape(primeiroNome(nome));
        return """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#151b26">
                  <h2 style="margin:0 0 16px;color:#0B1F3A">School<span style="color:#3A6EA5">Sync</span></h2>
                  <p>Olá, %s!</p>
                  <p>Use este código para redefinir sua senha:</p>
                  <p style="font-size:32px;font-weight:bold;letter-spacing:8px;margin:24px 0;color:#0B1F3A">%s</p>
                  <p>Ele vale por %d minutos.</p>
                  <p style="color:#5b6577;font-size:13px">Se você não pediu para trocar a senha, ignore este e-mail: sua senha continua a mesma.</p>
                </div>
                """.formatted(nomeSeguro, codigo, properties.validade());
    }
}
