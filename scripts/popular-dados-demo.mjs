// Popula a API do SchoolSync com dados de demonstração, passando pelas rotas da própria API
// (ou seja, todas as regras de negócio valem: limites de pontos, períodos, permissões...).
//
// Uso (Node 18 ou mais novo, sem instalar nada):
//   node scripts/popular-dados-demo.mjs --senha SENHA_DO_LIDER
//
// Opções:
//   --api URL              endereço da API (padrão: a do Render, ou a variável API_URL)
//   --email EMAIL          e-mail do líder da sala (padrão: victorhugomendesoares@gmail.com)
//   --nome NOME            nome do líder (padrão: Victor Hugo)
//   --senha SENHA          senha do líder (obrigatória, ou a variável SENHA_LIDER)
//   --senha-colegas SENHA  senha das contas dos colegas (padrão: schoolsync123)
//   --sala NOME            nome da sala (padrão: DS3)
//
// A senha do líder n fica no código de propósito, pra n ir parar no GitHub.
// Se o líder já tiver uma sala com esse nome, o script para sem duplicar nada.

const args = Object.fromEntries(
  process.argv.slice(2).reduce((pares, arg, i, todos) => {
    if (arg.startsWith('--')) pares.push([arg.slice(2), todos[i + 1]]);
    return pares;
  }, [])
);

const API = (args.api || process.env.API_URL || 'https://schoolsync-api-kvfx.onrender.com').replace(/\/$/, '');
const LIDER = {
  nome: args.nome || 'Victor Hugo',
  email: args.email || 'victorhugomendesoares@gmail.com',
  senha: args.senha || process.env.SENHA_LIDER,
};
const SENHA_COLEGAS = args['senha-colegas'] || 'schoolsync123';
const NOME_SALA = args.sala || 'DS3';

if (!LIDER.senha) {
  console.error('Informe a senha do líder: node scripts/popular-dados-demo.mjs --senha SUA_SENHA');
  process.exit(1);
}

// ---------------------------------------------------------------- dados

const COLEGAS = [
  { chave: 'ana', nome: 'Ana Souza', email: 'ana.souza@example.com' },
  { chave: 'bruno', nome: 'Bruno Lima', email: 'bruno.lima@example.com' },
  { chave: 'carla', nome: 'Carla Mendes', email: 'carla.mendes@example.com' },
  { chave: 'diego', nome: 'Diego Rocha', email: 'diego.rocha@example.com' },
];

const MATERIAS = {
  DBF: 'Desenvolvimento Baseado em Frameworks',
  MOB: 'Programação para Dispositivos Móveis',
  IHS: 'Infraestrutura de Hardware e Software II',
  MAT: 'Matemática',
  POR: 'Língua Portuguesa',
  FIS: 'Física',
};

// ano letivo de 2026 dividido em bimestres de 25 pontos
const PERIODOS = [
  { dataInicio: '2026-02-02', dataFim: '2026-04-30', pontuacaoMaxima: 25 },
  { dataInicio: '2026-05-04', dataFim: '2026-07-10', pontuacaoMaxima: 25 },
  { dataInicio: '2026-07-27', dataFim: '2026-10-02', pontuacaoMaxima: 25 },
  { dataInicio: '2026-10-05', dataFim: '2026-12-18', pontuacaoMaxima: 25 },
];

// soma por matéria em cada bimestre fica abaixo de 25 e nenhuma passa de 15 pontos
const ATIVIDADES = [
  // 3º bimestre (já passou)
  { chave: 'dbfP1', criador: 'lider', materia: 'DBF', data: '2026-08-20', valor: 10,
    titulo: 'Prova Prática I – Spring Boot e JPA',
    descricao: 'CRUD com Spring Data JPA, DTOs e validação. Individual, no laboratório.' },
  { chave: 'dbfP2', criador: 'lider', materia: 'DBF', data: '2026-09-28', valor: 15,
    titulo: 'Prova Prática II – API com JWT e perfis',
    descricao: 'API com Spring Security, autenticação JWT e controle de acesso por perfil.' },
  { chave: 'ihsProva', criador: 'bruno', materia: 'IHS', data: '2026-08-11', valor: 15,
    titulo: 'Prova prática – usuários, grupos e compartilhamentos',
    descricao: 'Windows Server 2019: criar usuários e grupos e configurar compartilhamentos.' },
  { chave: 'matLista', criador: 'carla', materia: 'MAT', data: '2026-08-14', valor: 5,
    titulo: 'Lista de exercícios – funções',
    descricao: 'Exercícios 1 a 20 da apostila. Entregar a resolução à mão.' },
  { chave: 'matTrigo', criador: 'carla', materia: 'MAT', data: '2026-09-18', valor: 15,
    titulo: 'Prova – trigonometria',
    descricao: 'Ciclo trigonométrico, identidades e equações.' },
  // 4º bimestre (o atual)
  { chave: 'mobShake', criador: 'lider', materia: 'MOB', data: '2026-10-05', valor: 10,
    titulo: 'Atividade 07 – Shake Challenge (acelerômetro)',
    descricao: 'App em Ionic que usa o acelerômetro do celular. Entregar o APK e o código.' },
  { chave: 'fisRelatorio', criador: 'bruno', materia: 'FIS', data: '2026-10-15', valor: 5,
    titulo: 'Relatório de laboratório – cinemática',
    descricao: 'Relatório do experimento do plano inclinado, em grupo de até 3 pessoas.' },
  { chave: 'porRedacao', criador: 'ana', materia: 'POR', data: '2026-10-16', valor: 10,
    titulo: 'Redação – texto dissertativo-argumentativo',
    descricao: 'Tema divulgado em sala. Mínimo de 25 linhas, no modelo do ENEM.' },
  { chave: 'ihsNtfs', criador: 'diego', materia: 'IHS', data: '2026-10-20', valor: 10,
    titulo: 'Trabalho – permissões NTFS e cotas de disco',
    descricao: 'Configurar permissões NTFS e cotas no servidor e entregar os prints.' },
  { chave: 'dbfProjeto', criador: 'lider', materia: 'DBF', data: '2026-10-21', valor: 15,
    titulo: 'Apresentação do projeto prático',
    descricao: 'Apresentar o projeto (API e app) e responder as perguntas do professor.' },
  { chave: 'porSeminario', criador: 'ana', materia: 'POR', data: '2026-10-27', valor: 10,
    titulo: 'Seminário – Vestido de Noiva (Nelson Rodrigues)',
    descricao: 'Seminário em grupo sobre a peça e os três planos da narrativa.' },
  { chave: 'matGeo', criador: 'carla', materia: 'MAT', data: '2026-11-06', valor: 15,
    titulo: 'Prova – geometria analítica',
    descricao: 'Ponto, reta e circunferência.' },
  { chave: 'fisNewton', criador: 'bruno', materia: 'FIS', data: '2026-11-13', valor: 15,
    titulo: 'Prova – dinâmica e leis de Newton',
    descricao: 'Leis de Newton, força de atrito e plano inclinado.' },
  { chave: 'mobFinal', criador: 'diego', materia: 'MOB', data: '2026-11-25', valor: 15,
    titulo: 'Projeto final – app com Capacitor',
    descricao: 'App completo usando pelo menos dois plugins do Capacitor.' },
];

// status no caderno do líder (as que n aparecem ficam como não iniciadas)
const STATUS_CADERNO_LIDER = {
  dbfP1: 'concluido', dbfP2: 'concluido', ihsProva: 'concluido', matLista: 'concluido',
  matTrigo: 'concluido', mobShake: 'concluido', fisRelatorio: 'em_andamento',
  porRedacao: 'em_andamento', dbfProjeto: 'em_andamento',
};

const GRUPOS = [
  {
    chave: 'projeto', criador: 'lider', nome: 'Projeto prático – apresentação', membros: ['ana', 'bruno'],
    tarefas: [
      { atividade: 'dbfProjeto', para: 'ana', status: 'em_andamento', titulo: 'Montar os slides da apresentação' },
      { atividade: 'dbfProjeto', para: 'lider', status: 'em_andamento', titulo: 'Preparar a demonstração do app e da API' },
      { atividade: 'dbfProjeto', para: 'bruno', status: 'concluido', titulo: 'Revisar o guia de estudo e as perguntas' },
      { atividade: 'dbfProjeto', para: 'lider', status: 'concluido', titulo: 'Criar os dados de demonstração' },
    ],
  },
  {
    chave: 'seminario', criador: 'ana', nome: 'Seminário – Vestido de Noiva', membros: ['lider', 'carla'],
    tarefas: [
      { atividade: 'porSeminario', para: 'lider', status: 'pendente', titulo: 'Pesquisar o contexto histórico da peça' },
      { atividade: 'porSeminario', para: 'carla', status: 'em_andamento', titulo: 'Resumir os planos da realidade, da memória e da alucinação' },
      { atividade: 'porSeminario', para: 'ana', status: 'pendente', titulo: 'Montar a apresentação do seminário' },
    ],
  },
  {
    chave: 'mobile', criador: 'diego', nome: 'Projeto final – Mobile', membros: ['lider'],
    tarefas: [
      { atividade: 'mobFinal', para: 'lider', status: 'pendente', titulo: 'Definir as telas e o fluxo do app' },
      { atividade: 'mobFinal', para: 'diego', status: 'pendente', titulo: 'Configurar o Capacitor e os plugins' },
    ],
  },
];

const COMENTARIOS = [
  { atividade: 'dbfProjeto', autor: 'ana', texto: 'Alguém sabe quanto tempo cada grupo vai ter pra apresentar?',
    resposta: { autor: 'lider', texto: 'O professor falou em uns 15 minutos, mais as perguntas no final.' } },
  { atividade: 'fisRelatorio', autor: 'bruno', texto: 'Os dados do experimento estão na pasta da turma, quem precisar é só pedir.' },
  { atividade: 'matGeo', autor: 'carla', texto: 'Vai cair circunferência também ou só reta?',
    resposta: { autor: 'diego', texto: 'Pelo que ele passou no quadro, cai as duas.' } },
];

// boletim do líder: notas das atividades que já passaram e algumas avulsas de bimestres anteriores
const NOTAS_ATIVIDADES = { dbfP1: 9, dbfP2: 14, ihsProva: 13.5, matLista: 5, matTrigo: 12, mobShake: 10 };
const NOTAS_AVULSAS = [
  { materia: 'MAT', data: '2026-03-20', valorMaximo: 15, valorObtido: 12.5, descricao: 'Prova – conjuntos e função do 1º grau' },
  { materia: 'FIS', data: '2026-04-10', valorMaximo: 10, valorObtido: 7.5, descricao: 'Prova – vetores e cinemática' },
  { materia: 'POR', data: '2026-06-12', valorMaximo: 15, valorObtido: 13, descricao: 'Prova – Modernismo' },
  { materia: 'DBF', data: '2026-06-26', valorMaximo: 15, valorObtido: 14, descricao: 'Prova – Spring MVC e API REST' },
];

// ---------------------------------------------------------------- acesso à API

async function chamar(metodo, caminho, { token, corpo, tentativas = 1 } = {}) {
  for (let tentativa = 1; ; tentativa++) {
    try {
      const resposta = await fetch(API + caminho, {
        method: metodo,
        headers: {
          ...(corpo !== undefined ? { 'Content-Type': 'application/json' } : {}),
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: corpo !== undefined ? JSON.stringify(corpo) : undefined,
        signal: AbortSignal.timeout(90_000),
      });
      const texto = await resposta.text();
      const dados = texto ? JSON.parse(texto) : null;
      if (!resposta.ok) {
        const erro = new Error(`${metodo} ${caminho} → ${resposta.status}: ${dados?.message || texto}`);
        erro.status = resposta.status;
        erro.mensagem = dados?.message || '';
        throw erro;
      }
      return dados;
    } catch (erro) {
      // só repete quando nem chegou resposta (ex: o Render ainda está acordando)
      if (erro.status || tentativa >= tentativas) throw erro;
      console.log(`  API não respondeu (${erro.message}), tentando de novo...`);
      await new Promise(r => setTimeout(r, 5_000));
    }
  }
}

async function garantirConta({ nome, email, senha }) {
  try {
    await chamar('POST', '/usuarios', { corpo: { nome, email, senha } });
    console.log(`  conta criada: ${email}`);
  } catch (erro) {
    if (!(erro.status === 400 && /cadastrado/i.test(erro.mensagem))) throw erro;
    console.log(`  conta já existia: ${email}`);
  }
  try {
    const login = await chamar('POST', '/usuarios/autenticar', { corpo: { email, senha } });
    return { token: login.token, id: login.usuario.id, nome };
  } catch (erro) {
    if (erro.status === 401) throw new Error(`A conta ${email} já existe com outra senha. Use a senha certa ou outro e-mail.`);
    throw erro;
  }
}

// ---------------------------------------------------------------- execução

async function main() {
  console.log(`API: ${API}`);
  console.log('Acordando a API (no plano gratuito do Render pode levar até 1 minuto)...');
  await chamar('GET', `/usuarios/verificar-login?email=${encodeURIComponent(LIDER.email)}`, { tentativas: 12 });

  console.log('Contas:');
  const pessoas = { lider: await garantirConta(LIDER) };
  for (const colega of COLEGAS) {
    pessoas[colega.chave] = await garantirConta({ ...colega, senha: SENHA_COLEGAS });
  }
  const token = chave => pessoas[chave].token;

  const salasDoLider = await chamar('GET', '/salas', { token: token('lider') });
  if (salasDoLider.some(sala => sala.nome === NOME_SALA)) {
    console.log(`\nO líder já tem uma sala "${NOME_SALA}". Nada foi alterado para não duplicar os dados.`);
    return;
  }

  console.log(`Sala "${NOME_SALA}"...`);
  const sala = await chamar('POST', '/salas', {
    token: token('lider'),
    corpo: {
      nome: NOME_SALA,
      tipoPeriodo: 'BIMESTRE',
      materias: Object.values(MATERIAS).map(nome => ({ nome })),
      periodos: PERIODOS,
    },
  });
  const idMateria = sigla => sala.materias.find(m => m.nome === MATERIAS[sigla]).id;

  for (const colega of COLEGAS) {
    await chamar('POST', `/salas/entrar?codigoConvite=${sala.codigoConvite}`, { token: token(colega.chave) });
  }
  console.log(`  código de convite: ${sala.codigoConvite} (${COLEGAS.length + 1} membros)`);

  console.log('Atividades...');
  const atividades = {};
  for (const a of ATIVIDADES) {
    const criada = await chamar('POST', '/atividades', {
      token: token(a.criador),
      corpo: {
        idSala: sala.id, idMateria: idMateria(a.materia), titulo: a.titulo,
        descricao: a.descricao, dataEntrega: a.data, valor: a.valor,
      },
    });
    atividades[a.chave] = { ...a, id: criada.id };
  }
  console.log(`  ${ATIVIDADES.length} atividades`);

  console.log('Caderno do líder...');
  for (const a of Object.values(atividades)) {
    if (a.criador !== 'lider') {
      // quem cria a atividade já entra com ela no caderno; as dos colegas o líder adiciona
      await chamar('POST', `/atividades/${a.id}/caderno`, { token: token('lider') });
    }
    const status = STATUS_CADERNO_LIDER[a.chave];
    if (status) {
      await chamar('PUT', `/atividades/${a.id}/status?status=${status}`, { token: token('lider') });
    }
  }

  console.log('Grupos e tarefas...');
  for (const g of GRUPOS) {
    const grupo = await chamar('POST', '/grupos', { token: token(g.criador), corpo: { nome: g.nome, idSala: sala.id } });
    for (const membro of g.membros) {
      await chamar('POST', `/grupos/entrar?codigoConvite=${grupo.codigoConvite}`, { token: token(membro) });
    }
    for (const t of g.tarefas) {
      const tarefa = await chamar('POST', `/grupos/${grupo.id}/tarefas`, {
        token: token(g.criador),
        corpo: { titulo: t.titulo, idAtividade: atividades[t.atividade].id, idUsuarioAtribuido: pessoas[t.para].id },
      });
      if (t.status !== 'pendente') {
        await chamar('PUT', `/tarefas/${tarefa.id}/status`, { token: token(t.para), corpo: { status: t.status } });
      }
    }
    console.log(`  ${g.nome}: ${g.membros.length + 1} membros, ${g.tarefas.length} tarefas`);
  }

  console.log('Comentários...');
  for (const c of COMENTARIOS) {
    const idAtividade = atividades[c.atividade].id;
    const comentario = await chamar('POST', `/atividades/${idAtividade}/comentarios`, {
      token: token(c.autor), corpo: { texto: c.texto },
    });
    if (c.resposta) {
      await chamar('POST', `/atividades/${idAtividade}/comentarios`, {
        token: token(c.resposta.autor), corpo: { texto: c.resposta.texto, idComentarioPai: comentario.id },
      });
    }
  }

  console.log('Boletim do líder...');
  for (const [chave, valorObtido] of Object.entries(NOTAS_ATIVIDADES)) {
    await chamar('POST', `/salas/${sala.id}/notas`, {
      token: token('lider'), corpo: { idAtividade: atividades[chave].id, valorObtido },
    });
  }
  for (const n of NOTAS_AVULSAS) {
    await chamar('POST', `/salas/${sala.id}/notas`, {
      token: token('lider'),
      corpo: { idMateria: idMateria(n.materia), descricao: n.descricao, valorMaximo: n.valorMaximo, data: n.data, valorObtido: n.valorObtido },
    });
  }
  console.log(`  ${Object.keys(NOTAS_ATIVIDADES).length} notas de atividades e ${NOTAS_AVULSAS.length} avulsas`);

  // lembrete 1 dia antes, pra já aparecer um lembrete no dia seguinte às 9h
  const configuracao = await chamar('GET', '/notificacoes/configuracoes', { token: token('lider') });
  await chamar('PUT', '/notificacoes/configuracoes', {
    token: token('lider'), corpo: { ...configuracao, lembreteDias: 1 },
  });

  console.log(`
Pronto! Dados de demonstração criados.
  Líder da sala "${NOME_SALA}": ${LIDER.email} (a senha que você informou)
  Colegas (senha ${SENHA_COLEGAS}): ${COLEGAS.map(c => c.email).join(', ')}
  Código de convite da sala: ${sala.codigoConvite}`);
}

main().catch(erro => {
  console.error(`\nFalhou: ${erro.message}`);
  process.exit(1);
});
