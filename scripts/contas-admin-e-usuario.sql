-- Contas para demonstrar os dois perfis no Trabalho Prático II (sem precisar do Insomnia).
-- Como rodar: abra o projeto no Neon, vá em "SQL Editor", cole este arquivo inteiro e clique em "Run".
--
--   vhadmin@gmail.com   / senha vhadmin123   -> perfil ADMIN   (vê o Painel do administrador)
--   vhusuario@gmail.com / senha vhusuario123 -> perfil USUARIO (recebe 403 ao tentar abrir /admin)
--
-- Não existe rota na API pra virar ADMIN (de propósito), por isso o admin é criado direto no banco.
-- As senhas já estão criptografadas com BCrypt, igual a API faz no cadastro.
-- Pode rodar mais de uma vez: se a conta já existir, ela só volta a ter a senha e o perfil abaixo.

INSERT INTO tb_usuario (id, nome, email, senha, ativo, perfil)
VALUES
  (gen_random_uuid()::text, 'VH Admin',   'vhadmin@gmail.com',   '$2a$10$VEuK7tSnqQJ4VJ6t3H4evuw8/mSDs2kDu8i80az7/yrhPhYe2mjOe', true, 'ADMIN'),
  (gen_random_uuid()::text, 'VH Usuário', 'vhusuario@gmail.com', '$2a$10$X1YZzbUJqkZ7Fw/xyD4B4.AceSxZfksSMmsP33YNVJoomeV8/HIt2', true, 'USUARIO')
ON CONFLICT (email) DO UPDATE
  SET senha  = EXCLUDED.senha,
      perfil = EXCLUDED.perfil,
      ativo  = true;

-- confere o resultado
SELECT nome, email, perfil, ativo
FROM tb_usuario
WHERE email IN ('vhadmin@gmail.com', 'vhusuario@gmail.com');
