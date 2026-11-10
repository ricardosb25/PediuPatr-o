# Dashboard Administrativo — Pediu Patrão

## 1. Escopo e decisões de análise

Painel administrativo integrado aos pedidos persistidos no MongoDB. Destinado a ADMIN e GERENTE, com AdminLTE 3.2.0, Chart.js 2.9.4 e JavaScript nativo. Não inclui vídeo, operações Git nem publicação no GitHub, conforme o escopo solicitado.

O painel separa desempenho comercial de acompanhamento operacional: receita/ticket consideram apenas vendas finalizadas, enquanto quantidade, cancelamentos e andamento consideram todos os pedidos selecionados. A operação atual não possui horário de finalização; por isso o agrupamento é uma **coorte pela entrada**, e não fluxo de caixa ou receita por data de pagamento. Um pedido que muda de status altera retrospectivamente os números do seu dia de entrada. Essa escolha é indicada na interface.

## 2. Requisitos funcionais

| Requisito | Comportamento e critério de aceitação |
| --- | --- |
| RF01 — Indicadores gerais | Mostrar faturamento líquido, total de pedidos, ticket médio e pedidos em andamento; complementar com finalizados, descontos, cancelados e taxa de cancelamento. |
| RF02 — Análise de vendas | Gráfico diário de receita e quantidade de vendas com valor válido, com eixos identificados. Preencher dias sem vendas com zero e fornecer tabela acessível com valores exatos. |
| RF03 — Situação dos pedidos | Gráfico de rosca e lista de quantidades por estado, incluindo zeros. Status desconhecido aparece como Não informado e gera aviso. Listar as 8 entradas mais recentes no filtro. |
| RF04 — Produtos mais vendidos | Ranking dos 10 produtos com maior quantidade em pedidos finalizados, identificação pelo produtoId e valor bruto histórico. Ordenar por quantidade decrescente, depois valor bruto decrescente e ID crescente. |
| RF05 — Filtros | Datas inicial/final e status único opcional, aplicados a todos os componentes. Atalhos Hoje, 7 dias e 30 dias. Preservar o último filtro válido na URL; consulta padrão dos últimos 30 dias inclusivos. |
| RF06 — Integração com API | Carregar opções via GET /api/dashboard/filtros e todos os dados via GET /api/dashboard/resumo. A tela não calcula indicadores comerciais a partir de listas de pedidos. |
| RF07 — Atualização e dados | Atualização manual e automática a cada 60 segundos, pausável e suspensa em abas ocultas. Exibir carregamento, data/hora de consulta, estado vazio, avisos de dados incompletos, falhas e sessão expirada. Preservar o último resultado após erro. |

## 3. Requisitos não funcionais

| Requisito | Implementação/limite verificável |
| --- | --- |
| RNF01 — Autorização | Sessão Spring Security; /dashboard e /api/dashboard/** exclusivos de ADMIN/GERENTE. Proteção adicional no DashboardService com @PreAuthorize. ATENDENTE recebe 403; API sem sessão, 401. |
| RNF02 — Privacidade | API não retorna clientes, endereços, telefones, senhas nem dados de usuários. Pedidos recentes têm somente ID, estado, entrada e total. Cache-Control: no-store em respostas do controlador e erros tratados. |
| RNF03 — Desempenho | Uma agregação MongoDB com filtro inicial e $facet por consulta. Índice dataEntrada/status, ranking limitado a 10 e recentes a 8. Janela máxima de 366 dias. Timeout Mongo de 10 segundos e navegador de 15 segundos. Latência sob carga não foi medida. |
| RNF04 — Precisão | Cálculos no banco com Decimal128 e no serviço com BigDecimal. Respostas monetárias com escala 2 e HALF_UP. Valores negativos, ausentes, não numéricos ou não finitos não viram receita. |
| RNF05 — Acessibilidade e adaptação | Layout responsivo, navegação por teclado, foco visível, rótulos, região de avisos, link para pular conteúdo, tabelas/listas alternativas aos gráficos e respeito à preferência de movimento reduzido. Conferência em desktop e largura móvel de 375 px úteis. |
| RNF06 — Manutenibilidade | Controlador, DTOs, serviço e contrato de repositório separados; implementação Mongo específica; Clock injetado para testes determinísticos. |
| RNF07 — Dependências locais | CSS/JS do AdminLTE, Bootstrap 4, jQuery, Font Awesome e Chart.js servidos do próprio projeto. O painel não mistura Bootstrap 5 do layout anterior com Bootstrap 4. |
| RNF08 — Robustez | Filtros inválidos retornam 400 antes da consulta; indisponibilidade de persistência retorna 503 sem detalhes internos. Requisições antigas são abortadas; não há atualizações automáticas simultâneas. |

## 4. Definições dos indicadores

- **Período:** dataEntrada entre a meia-noite local de inicio, inclusiva, e a meia-noite do dia seguinte a fim, exclusiva. O fuso é America/Sao_Paulo, incluindo regras históricas de horário de verão. Datas entre 2000-01-01 e 2100-12-31, em ordem, com até 366 dias inclusivos. Registros sem dataEntrada ficam fora da consulta; não se inventa histórico.
- **Status:** todos, quando omitido; caso informado, o filtro é aplicado antes de todos os cálculos, inclusive ranking e recentes. Filtrar CANCELADO faz faturamento/ticket/ranking ficarem zerados/vazios, pois não há venda finalizada nesse recorte.
- **Faturamento líquido:** soma dos valores totais finitos e não negativos dos pedidos FINALIZADO selecionados. Total zero é uma venda válida.
- **Ticket médio:** faturamento líquido dividido pelo número de finalizados com total válido; zero quando não há denominador. Pedidos finalizados com valor ausente/inválido são contados em pedidosSemValor e excluídos do denominador.
- **Descontos concedidos:** soma de subtotal - valorTotal somente para vendas válidas com subtotal maior ou igual ao total; subtotal ausente/inválido não gera desconto fictício.
- **Em andamento:** RECEBIDO, EM_PREPARACAO, PRONTO, SAIU_PARA_ENTREGA e RETIRADO.
- **Taxa de cancelamento:** cancelados / total selecionado × 100, com duas casas; zero para período vazio. Trata-se da proporção dentro do filtro; ao filtrar apenas cancelados, será 100% quando houver pedidos.
- **Série diária:** receita e quantidade apenas de finalizados com total válido, por entrada local. Dias sem vendas retornam zero.
- **Ranking:** soma de quantidades positivas e inteiras dos itens de pedidos finalizados, agrupada por produtoId não vazio. Usa nome e preço registrados no pedido, sem consultar o preço atual do catálogo. Nome apresentado é o da venda mais recente. O valor bruto é quantidade × preço unitário finito e não negativo, antes de descontos; não deve ser comparado diretamente à receita líquida. Quantidades válidas podem contribuir mesmo quando o total do pedido ou preço do item é legado/incompleto. Itens sem ID/quantidade válidos são ignorados. Pedidos sem uma lista de itens ou com lista vazia geram aviso.
- **Compatibilidade:** EM_PREPARO e EM PREPARAÇÃO normalizam para EM_PREPARACAO; ENTREGUE para FINALIZADO; SAIU PARA ENTREGA para SAIU_PARA_ENTREGA. Estados restantes desconhecidos normalizam para NAO_INFORMADO e não contam como venda nem andamento.
- **Atualização:** atualizadoEm indica quando a resposta foi calculada, não a última alteração de um pedido. O painel não efetua mutações nem escreve auditoria por consultar os dados.

## 5. Contrato HTTP

Base: mesma origem da aplicação. Autenticação por sessão JSESSIONID obtida no login normal. Todas as rotas deste módulo são GET; nenhuma alteração de pedido é feita. O logout continua POST com CSRF, gerado pelo Thymeleaf.

### GET /dashboard

Entrega a página HTML para ADMIN e GERENTE. Sem sessão, redireciona ao login. ATENDENTE recebe 403. A página busca os dados após carregar.

### GET /api/dashboard/filtros

Não recebe parâmetros. Retorna HTTP 200 e Cache-Control: no-store, com datas padrão e opções dos filtros. Não consulta a coleção de pedidos.

Requisição (usando uma sessão já autenticada):

```http
GET /api/dashboard/filtros HTTP/1.1
Accept: application/json
Cookie: JSESSIONID=<sessão autenticada>
```

Exemplo ilustrativo de resposta para 06/11/2026, com valores de calendário e sem dados comerciais reais:

```json
{
  "inicioPadrao": "2026-10-08",
  "fimPadrao": "2026-11-06",
  "fusoHorario": "America/Sao_Paulo",
  "maximoDias": 366,
  "atualizacaoSegundos": 60,
  "status": [
    {"valor": "RECEBIDO", "rotulo": "Recebido"},
    {"valor": "EM_PREPARACAO", "rotulo": "Em preparação"},
    {"valor": "PRONTO", "rotulo": "Pronto"},
    {"valor": "SAIU_PARA_ENTREGA", "rotulo": "Saiu para entrega"},
    {"valor": "RETIRADO", "rotulo": "Retirado"},
    {"valor": "FINALIZADO", "rotulo": "Finalizado"},
    {"valor": "CANCELADO", "rotulo": "Cancelado"}
  ]
}
```

### GET /api/dashboard/resumo

| Parâmetro | Tipo | Obrigatório | Regra |
| --- | --- | --- | --- |
| inicio | data AAAA-MM-DD | Não | Se omitido, usa hoje menos 29 dias no fuso da operação. |
| fim | data AAAA-MM-DD | Não | Se omitido, usa hoje no fuso da operação. |
| status | enum | Não | Um dos sete valores canônicos de filtros; não aceita aliases legados na requisição. Omitir para todos. |

Os padrões das duas datas são independentes; se apenas uma data for informada, o intervalo resultante ainda deve respeitar a validação. Não há parâmetro de total, preço, usuário ou expressão MongoDB.

Exemplos de requisição:

```http
GET /api/dashboard/resumo?inicio=2026-11-01&fim=2026-11-02 HTTP/1.1
Accept: application/json
Cookie: JSESSIONID=<sessão autenticada>
```

```http
GET /api/dashboard/resumo?inicio=2026-11-01&fim=2026-11-06&status=FINALIZADO HTTP/1.1
Accept: application/json
Cookie: JSESSIONID=<sessão autenticada>
```

Resposta HTTP 200 (exemplo **fictício apenas para documentação**, sem inserção no banco):

```json
{
  "filtro": {
    "inicio": "2026-11-01",
    "fim": "2026-11-02",
    "status": null,
    "fusoHorario": "America/Sao_Paulo"
  },
  "indicadores": {
    "totalPedidos": 2,
    "pedidosFinalizados": 1,
    "pedidosCancelados": 1,
    "pedidosEmAndamento": 0,
    "pedidosComValor": 1,
    "pedidosSemValor": 0,
    "pedidosSemItens": 0,
    "faturamentoLiquido": 90.0,
    "ticketMedio": 90.0,
    "descontosConcedidos": 10.0,
    "taxaCancelamento": 50.0
  },
  "vendasPorDia": [
    {
      "data": "2026-11-01",
      "pedidos": 1,
      "faturamentoLiquido": 90.0
    },
    {
      "data": "2026-11-02",
      "pedidos": 0,
      "faturamentoLiquido": 0.0
    }
  ],
  "situacaoPedidos": [
    {
      "status": "RECEBIDO",
      "quantidade": 0
    },
    {
      "status": "EM_PREPARACAO",
      "quantidade": 0
    },
    {
      "status": "PRONTO",
      "quantidade": 0
    },
    {
      "status": "SAIU_PARA_ENTREGA",
      "quantidade": 0
    },
    {
      "status": "RETIRADO",
      "quantidade": 0
    },
    {
      "status": "FINALIZADO",
      "quantidade": 1
    },
    {
      "status": "CANCELADO",
      "quantidade": 1
    }
  ],
  "produtosMaisVendidos": [
    {
      "produtoId": "produto-exemplo",
      "nome": "Pizza Calabresa",
      "quantidade": 2,
      "valorBruto": 100.0
    }
  ],
  "pedidosRecentes": [
    {
      "id": "pedido-cancelado-exemplo",
      "status": "CANCELADO",
      "dataEntrada": "2026-11-02T20:00:00Z",
      "valorTotal": 50.0
    },
    {
      "id": "pedido-finalizado-exemplo",
      "status": "FINALIZADO",
      "dataEntrada": "2026-11-01T20:00:00Z",
      "valorTotal": 90.0
    }
  ],
  "avisos": [],
  "atualizadoEm": "2026-11-06T23:00:00Z"
}
```

Os números JSON podem omitir zeros finais na serialização ou no cliente. Na interface, moeda usa formato BRL/pt-BR. valorTotal de pedido recente pode ser null; a tela mostra Sem valor válido. A distribuição inclui todos os sete estados, mesmo com zero; NAO_INFORMADO só aparece quando necessário. Não há paginação: série limitada a 366 dias, ranking a 10 e recentes a 8.

### Erros possíveis

| HTTP | Causa | Tratamento |
| --- | --- | --- |
| 400 | Data impossível, status inválido, início posterior ao fim, mais de 366 dias ou datas fora dos limites | JSON com erro textual. Não executa a consulta ao MongoDB. |
| 401 | Sessão ausente/expirada ou usuário removido | Exigir novo login; a UI pausa atualização automática e oferece acesso ao login. |
| 403 | Perfil sem permissão | Acesso negado pelo Spring Security; a UI informa falta de acesso. |
| 405 | Método HTTP não implementado | O módulo não oferece escrita. Em métodos mutantes sem CSRF válido, Spring Security pode retornar 403 antes do controlador. |
| 503 | Persistência indisponível ou erro de acesso traduzido pelo Spring Data | JSON sem URI, credenciais, consulta ou stack trace. Preserva últimos dados no navegador. |

Exemplos:

```json
{"erro":"Filtro inválido: use datas AAAA-MM-DD e um status permitido."}
```

```json
{"erro":"Informe início e fim entre 2000 e 2100, em ordem, com no máximo 366 dias inclusivos."}
```

```json
{"erro":"Não foi possível consultar os pedidos. Tente novamente em instantes."}
```

Erros 401/403 originados no filtro de segurança não têm contrato JSON específico; o cliente trata seu status. Falhas de rede e timeout local são informados pela interface, sem fabricar resposta da API.

## 6. Projeto e integração com o banco

Fluxo: dashboard.html → dashboard.js → DashboardController → DashboardService → DashboardRepository → MongoDashboardRepository → coleção pedidos.

O repositório usa uma agregação com $match inicial de período/status, normalização de estados e decimais, e $facet para cinco grupos: totais, vendasPorDia, situacaoPedidos, produtosMaisVendidos e pedidosRecentes. Os componentes partem do mesmo conjunto filtrado, evitando cinco buscas completas ou agregação em memória de todos os pedidos. Não é uma garantia de snapshot transacional contra escritas concorrentes: o painel é uma consulta operacional, sem alteração de dados.

O índice `dashboard_periodo_status` em dataEntrada/status é criado na inicialização junto dos índices existentes. Não foi criada outra coleção, nem alterado o modelo Pedido. Conversões permitem ler o BigDecimal histórico persistido como texto pelo Spring Data, além de tipos numéricos BSON.

Para executar a aplicação, use as variáveis do README: MONGODB_URI e, se não houver ADMIN, BOOTSTRAP_ADMIN_USERNAME/BOOTSTRAP_ADMIN_PASSWORD. MONGODB_DATABASE é opcional. O sistema de pedidos/auditoria continua exigindo Atlas ou replica set pelas transações já existentes. A agregação do dashboard usa operadores disponíveis no MongoDB 4.0+, mas a versão efetiva deve atender ao suporte do driver/Spring usado no projeto.

**Limite desta entrega:** MONGODB_URI não estava configurada, então não foi feita conexão com banco real nem validação de desempenho sob carga. A consulta foi compilada e seu contrato/mapeamento foi testado com MongoTemplate mockado. Existem testes optativos que executam a agregação em MongoDB real, descritos a seguir.

## 7. Evidências e reprodução dos testes

### Suíte Java

```powershell
mvn test
```

Suíte Maven: 33 testes encontrados; 30 executados e aprovados; 3 desativados; 0 falhas; 0 erros. Zero falhas e zero erros. Os testes desativados são os três de integração real com MongoDB.

| Arquivo | Cobertura |
| --- | --- |
| DashboardServiceTest.java | Datas padrão no fuso correto, período inclusivo, preenchimento de dias sem venda, ticket e cancelamento, zeros, avisos, status e ano bissexto/limites. |
| MongoDashboardRepositoryTest.java | Envio de período/status ao MongoTemplate, aliases legados, organização das facetas, leitura de Decimal128, ranking/recentes e resposta vazia. Não executa operadores no servidor Mongo. |
| DashboardControllerTest.java | API 200 para ADMIN/GERENTE, 401/403, datas/status inválidos 400, erro 503 sem detalhes internos, cabeçalhos, assets AdminLTE e geração da prévia isolada. Usa serviço real com repositório mockado. |
| MongoDashboardIntegrationTest.java | Testes optativos de agregação em banco real: valores texto/Decimal128/NaN, status legados, limites de data, finalizados/cancelados, ranking e coleção vazia. |

A suíte também executa os 15 testes anteriores de pedidos, clientes, segurança e templates. Evidência resumida exportada dos relatórios Surefire: [testes-maven.txt](evidencias/testes-maven.txt). Os XML e relatórios completos ficam em target/surefire-reports.

### Testes optativos com MongoDB real

Configure **DASHBOARD_TEST_MONGODB_URI**, distinta da configuração da aplicação, com uma conta de ambiente de testes capaz de criar/remover bancos. O teste cria um banco `pediupatrao_dashboard_test_` seguido de UUID, insere dados sintéticos e remove apenas esse banco no encerramento. Não usa MONGODB_DATABASE e não reutiliza a coleção da aplicação.

```powershell
$env:DASHBOARD_TEST_MONGODB_URI = Read-Host 'URI do MongoDB de testes' -MaskInput
mvn '-Dtest=MongoDashboardIntegrationTest' test
Remove-Item Env:DASHBOARD_TEST_MONGODB_URI
```

Sem a variável, os três testes são explicitamente skipped. Eles não são contabilizados como testes aprovados nesta entrega.

### Conferência do navegador

A prévia visual é gerada por DashboardControllerTest em target/dashboard-preview, usando mocks e dados sintéticos. O servidor `src/test/js/dashboard-preview-server.cjs` serve apenas esses arquivos e assets locais em 127.0.0.1:18765. Ele **não é o backend nem valida filtros contra o banco**: serve respostas fixas só para conferir renderização/estados da UI.

```powershell
mvn '-Dtest=DashboardControllerTest' test
node src/test/js/dashboard-preview-server.cjs
# Para outras conferências, encerre com Ctrl+C e escolha um cenário:
node src/test/js/dashboard-preview-server.cjs --vazio
node src/test/js/dashboard-preview-server.cjs --erro
```

Verificações realizadas na prévia:

- Desktop: indicadores, dois gráficos, 30 linhas diárias, ranking de 5 produtos sintéticos e recentes renderizados; sem erro de console.
- Móvel: largura útil de 375 px, sem rolagem horizontal do documento; menu AdminLTE abre/fecha. O ranking admite rolagem interna para preservar as colunas.
- Vazio: moeda em zero, mensagem de ausência, ranking vazio e distribuição sem rosca enganosa.
- Erro: resposta 503 simulada após resultado válido; últimos valores preservados e aviso DADOS ANTERIORES · ATUALIZAÇÃO PENDENTE.
- Sintaxe: node --check para dashboard.js e servidor de prévia.

Todas as capturas abaixo usam **dados sintéticos**, sem resultados comerciais reais:

- [Desktop](evidencias/dashboard-desktop.png)
- [Móvel](evidencias/dashboard-mobile.png)
- [Sem pedidos](evidencias/dashboard-vazio.png)
- [Falha de atualização](evidencias/dashboard-erro.png)

## 8. Arquivos e métodos

O inventário completo desta implementação está em [dashboard-walkthrough.md](dashboard-walkthrough.md). O README da aplicação contém o ponto de entrada e execução. A documentação e evidências estão prontas para revisão e posterior publicação pelo responsável pelo repositório.
