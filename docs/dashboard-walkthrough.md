# Walkthrough — Dashboard AdminLTE

## O que mudou

Painel em /dashboard, para ADMIN e GERENTE, alimentado por duas APIs novas e agregações da coleção pedidos. Usa os arquivos locais de AdminLTE 3.2 e Chart.js 2.9. A interface apresenta indicadores comerciais/operacionais, evolução diária, distribuição de status, ranking, últimos pedidos e filtros de período/status. Atualiza a cada 60 segundos, permite pausar, trata ausência/erro de dados e preserva o último resultado em falhas.

Não foram feitas operações Git, gravação de vídeo nem alterações no banco real. As permissões e regras dos pedidos já existentes foram preservadas; adicionou-se apenas autorização de consulta ao dashboard.

## Arquivos criados nesta etapa

- `src/main/java/com/umc/pediupatrao/config/DashboardConfig.java`
- `src/main/java/com/umc/pediupatrao/controller/DashboardController.java`
- `src/main/java/com/umc/pediupatrao/controller/DashboardExceptionHandler.java`
- `src/main/java/com/umc/pediupatrao/dto/DashboardAgregado.java`
- `src/main/java/com/umc/pediupatrao/dto/DashboardFiltros.java`
- `src/main/java/com/umc/pediupatrao/dto/DashboardResumo.java`
- `src/main/java/com/umc/pediupatrao/repository/DashboardRepository.java`
- `src/main/java/com/umc/pediupatrao/repository/MongoDashboardRepository.java`
- `src/main/java/com/umc/pediupatrao/service/DashboardService.java`
- `src/main/resources/templates/dashboard.html`
- `src/main/resources/static/css/dashboard.css`
- `src/main/resources/static/js/dashboard.js`
- `src/test/java/com/umc/pediupatrao/DashboardServiceTest.java`
- `src/test/java/com/umc/pediupatrao/MongoDashboardRepositoryTest.java`
- `src/test/java/com/umc/pediupatrao/DashboardControllerTest.java`
- `src/test/java/com/umc/pediupatrao/MongoDashboardIntegrationTest.java`
- `src/test/js/dashboard-preview-server.cjs`
- `docs/dashboard.md`
- `docs/dashboard-walkthrough.md`
- `docs/evidencias/testes-maven.txt`
- `docs/evidencias/dashboard-desktop.png`
- `docs/evidencias/dashboard-mobile.png`
- `docs/evidencias/dashboard-vazio.png`
- `docs/evidencias/dashboard-erro.png`

## Arquivos existentes modificados nesta etapa

- `README.md`
- `WALKTHROUGH.md`
- `src/main/java/com/umc/pediupatrao/config/SecurityConfig.java`
- `src/main/java/com/umc/pediupatrao/config/MongoInitConfig.java`
- `src/main/resources/templates/fragments/sidebar.html`

SecurityConfig adiciona as rotas administrativas do painel. MongoInitConfig cria o índice dashboard_periodo_status. O fragmento de navegação adiciona o link por perfil; README/WALKTHROUGH apontam para os novos documentos. Nenhuma função nova foi adicionada a esses arquivos existentes.

## Todas as novas funções, métodos, interface e construtores explícitos

Caminhos Java relativos a `src/main/java/com/umc/pediupatrao/`:

| Arquivo | Novos membros |
| --- | --- |
| `config/DashboardConfig.java` | `dashboardClock()` |
| `controller/DashboardController.java` | construtor `DashboardController(DashboardService)`; `pagina()`, `filtros()`, `resumo(LocalDate, LocalDate, StatusPedido)` |
| `controller/DashboardExceptionHandler.java` | `filtroInvalido(Exception)`, `bancoIndisponivel()` |
| `repository/DashboardRepository.java` | nova interface `DashboardRepository`; contrato `consultar(Instant, Instant, StatusPedido)` |
| `repository/MongoDashboardRepository.java` | construtor `MongoDashboardRepository(MongoTemplate)`; `consultar(Instant, Instant, StatusPedido)`, `valoresPersistidos(StatusPedido)`, `statusNormalizado()`, `decimal(String)`, `somaCondicional(Object, Object)`, `totais()`, `vendasPorDia()`, `produtosMaisVendidos()`, `mapear(Document)`, `documentos(Document, String)`, `numero(Document, String)`, `dinheiro(Object)` |
| `service/DashboardService.java` | construtor `DashboardService(DashboardRepository, Clock)`; `filtros()`, `consultar(LocalDate, LocalDate, StatusPedido)`, `validarPeriodo(LocalDate, LocalDate)`, `moeda(BigDecimal)`, `rotulo(StatusPedido)` |

Em `src/main/resources/static/js/dashboard.js`:

- `texto()`
- `elemento()`
- `dataCurta()`
- `dataCompleta()`
- `instanteLocal()`
- `rotuloStatus()`
- `buscarJson()`
- `mostrarErro()`
- `marcarCarregamento()`
- `atualizarDashboard()`
- `renderizarResumo()`
- `renderizarVendas()`
- `renderizarStatus()`
- `renderizarRanking()`
- `renderizarRecentes()`
- `desenharGraficos()`
- `rotuloTooltipVenda()`
- `rotuloEixoReceita()`
- `rotuloTooltipStatus()`
- `aplicarPeriodo()`
- `inicializarDashboard()`
- `aplicarFiltros()`
- `atualizarManualmente()`
- `encerrarAtualizacao()`

As funções são internas ao módulo do dashboard. Também há callbacks anônimos de transformação das séries/listas, seleção dos atalhos, controle de timeout e intervalo automático; não expõem API global.

Em `src/test/js/dashboard-preview-server.cjs`: callbacks nomeados `servirPrevia(requisicao, resposta)` e `avisarPrevia()`. Esse servidor é exclusivo da prévia sintética e não é uma API da aplicação.

### Records e métodos gerados

Cada record abaixo adiciona seu construtor canônico, `equals(Object)`, `hashCode()`, `toString()` e os acessores com os nomes indicados:

| Arquivo | Record | Acessores |
| --- | --- | --- |
| `dto/DashboardAgregado.java` | `DashboardAgregado` | `totais()`, `vendasPorDia()`, `situacaoPedidos()`, `produtosMaisVendidos()`, `pedidosRecentes()` |
| `dto/DashboardAgregado.java` | `Totais` | `totalPedidos()`, `pedidosFinalizados()`, `pedidosCancelados()`, `pedidosEmAndamento()`, `pedidosComValor()`, `pedidosSemValor()`, `pedidosSemItens()`, `faturamentoLiquido()`, `descontosConcedidos()` |
| `dto/DashboardFiltros.java` | `DashboardFiltros` | `inicioPadrao()`, `fimPadrao()`, `fusoHorario()`, `maximoDias()`, `atualizacaoSegundos()`, `status()` |
| `dto/DashboardFiltros.java` | `OpcaoStatus` | `valor()`, `rotulo()` |
| `dto/DashboardResumo.java` | `DashboardResumo` | `filtro()`, `indicadores()`, `vendasPorDia()`, `situacaoPedidos()`, `produtosMaisVendidos()`, `pedidosRecentes()`, `avisos()`, `atualizadoEm()` |
| `dto/DashboardResumo.java` | `FiltroAplicado` | `inicio()`, `fim()`, `status()`, `fusoHorario()` |
| `dto/DashboardResumo.java` | `Indicadores` | `totalPedidos()`, `pedidosFinalizados()`, `pedidosCancelados()`, `pedidosEmAndamento()`, `pedidosComValor()`, `pedidosSemValor()`, `pedidosSemItens()`, `faturamentoLiquido()`, `ticketMedio()`, `descontosConcedidos()`, `taxaCancelamento()` |
| `dto/DashboardResumo.java` | `VendaDiaria` | `data()`, `pedidos()`, `faturamentoLiquido()` |
| `dto/DashboardResumo.java` | `SituacaoPedido` | `status()`, `quantidade()` |
| `dto/DashboardResumo.java` | `ProdutoMaisVendido` | `produtoId()`, `nome()`, `quantidade()`, `valorBruto()` |
| `dto/DashboardResumo.java` | `PedidoRecente` | `id()`, `status()`, `dataEntrada()`, `valorTotal()` |

### Métodos de testes adicionados

Caminhos relativos a `src/test/java/com/umc/pediupatrao/`:

- `DashboardServiceTest.java`: `preparar()`, `agregadoVazio()`, `usaTrintaDiasEFusoDeSaoPaulo()`, `calculaTicketTaxaEPrencheDiasSemVenda()`, `evitaDivisaoPorZeroSemInventarPedidos()`, `sinalizaLegadosEStatusNaoReconhecidos()`, `rejeitaIntervalosInvalidosAntesDeConsultarBanco()`, `aceitaAnoBissextoEEncaminhaStatus()`.
- `MongoDashboardRepositoryTest.java`: `enviaPeriodoEStatusAoMongoEMapeiaDecimais()`, `aceitaResultadoVazioSemEncontrarDocumentos()`.
- `DashboardControllerTest.java`: `preparar()`, `adminEGerenteConsultamApiSemExporDadosPessoais()`, `filtrosExibemSeteEstadosELimites()`, `exigeAutenticacaoEBloqueiaAtendente()`, `filtrosInvalidosRetornam400()`, `indisponibilidadeRetorna503SemDetalhesInternos()`, `geraPreviaVisualIsolada()`, `paginaUsaAdminLteEAssetsLocais()`.
- `MongoDashboardIntegrationTest.java`: `prepararBancoIsolado()`, `removerSomenteBancoDoTeste()`, `pedido()`, `agregaNoMongoRespeitandoPeriodoEstadosEValoresLegados()`, `statusFiltraTodasAsFacetasEAceitaAliasLegado()`, `colecaoVaziaRetornaZeros()`.

## Mocks, dados sintéticos e ambiente

- `DashboardServiceTest.java`: mock do contrato de repositório, relógio fixo e agregados sintéticos para verificar regras e períodos.
- `MongoDashboardRepositoryTest.java`: MongoTemplate mockado e documentos BSON sintéticos para verificar envio/mapeamento da agregação, sem acesso a MongoDB real.
- `DashboardControllerTest.java`: repositório e autenticação mockados, serviço real; dados sintéticos apenas para a prévia/contratos HTTP. Gera `target/dashboard-preview/dashboard.html`, `filtros.json`, `resumo.json` e `resumo-vazio.json`.
- `MongoDashboardIntegrationTest.java`: não usa mocks de persistência; insere dados sintéticos somente em banco exclusivo descartável quando `DASHBOARD_TEST_MONGODB_URI` for fornecida. Não executado nesta sessão por ausência da variável.
- `dashboard-preview-server.cjs`: serve as respostas fixas acima, ou resposta 503 simulada, apenas na interface de loopback. Não usa autenticação real nem substitui o backend. Foi encerrado após a conferência visual.
- Os exemplos de JSON em `dashboard.md` e as capturas em `docs/evidencias` são explicitamente sintéticos. Não foram criados produtos, pedidos ou usuários demonstrativos na aplicação.

Faltava `MONGODB_URI` para iniciar o sistema contra um banco real e `DASHBOARD_TEST_MONGODB_URI` para os testes de integração. Para primeiro administrador, continuam aplicáveis `BOOTSTRAP_ADMIN_USERNAME` e `BOOTSTRAP_ADMIN_PASSWORD`. As configurações não foram inventadas ou preenchidas com credenciais fictícias.

## Validação

33 testes encontrados: 30 executados/aprovados, 3 desativados, zero falhas e erros. Sintaxe dos dois arquivos JavaScript validada com node --check. A prévia foi conferida em desktop/móvel e nos estados vazio e erro; esses testes visuais usaram dados sintéticos. Não houve comprovação de execução da agregação em MongoDB real nesta sessão.

- [Requisitos e contrato HTTP](dashboard.md)
- [Resultado exportado dos relatórios](evidencias/testes-maven.txt)
- [Prévia desktop sintética](evidencias/dashboard-desktop.png)
