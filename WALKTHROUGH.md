# Walkthrough das alterações

## Resultado e validação

Implementados os três perfis com autorização no backend, controle do fluxo de pedidos, cálculo monetário no servidor, descontos e cancelamento exclusivos de GERENTE, auditoria persistente e consulta protegida. Transações MongoDB unem cada alteração à sua auditoria. A aplicação mantém as camadas e tecnologias originais.

Validação executada: `mvn -B -o test` com JDK 21 — **15 testes, zero falhas, zero erros**. Foram testadas regras de pedidos, auditoria de clientes, exclusão de cliente com histórico, proteção de dados, permissões HTTP, CSRF e renderização Thymeleaf. A integração com MongoDB/replica set e o rollback real de transações não foram executados, porque não foi configurada uma conexão para essa validação. Nenhuma conexão com o banco anterior foi executada. Vídeo e publicação GitHub permanecem tarefas individuais do aluno.

## Todos os arquivos criados

- `README.md`: execução, requisitos de ambiente, matriz de permissões, APIs e roteiro de vídeo.
- `WALKTHROUGH.md`: este inventário.
- `src/main/java/com/umc/pediupatrao/config/MongoTransactionConfig.java`
- `src/main/java/com/umc/pediupatrao/config/PerfilAtualFilter.java`
- `src/main/java/com/umc/pediupatrao/config/StatusPedidoReadConverter.java`
- `src/main/java/com/umc/pediupatrao/controller/AuditoriaController.java`
- `src/main/java/com/umc/pediupatrao/controller/ErroControllerAdvice.java`
- `src/main/java/com/umc/pediupatrao/dto/NovoPedido.java`
- `src/main/java/com/umc/pediupatrao/entity/Auditoria.java`
- `src/main/java/com/umc/pediupatrao/entity/ItemPedido.java`
- `src/main/java/com/umc/pediupatrao/entity/StatusPedido.java`
- `src/main/java/com/umc/pediupatrao/repository/AuditoriaRepository.java`
- `src/main/java/com/umc/pediupatrao/service/AuditoriaService.java`
- `src/main/java/com/umc/pediupatrao/service/OperadorService.java`
- `src/main/resources/static/js/api.js`
- `src/main/resources/static/js/pedidos.js`
- `src/main/resources/templates/auditoria.html`
- `src/test/java/com/umc/pediupatrao/ClienteAuditoriaTest.java`
- `src/test/java/com/umc/pediupatrao/PedidoServiceTest.java`
- `src/test/java/com/umc/pediupatrao/SegurancaTest.java`
- `src/test/java/com/umc/pediupatrao/TelasTest.java`

## Todos os arquivos existentes modificados

- `pom.xml`: dependências de teste e dialeto Thymeleaf de segurança.
- `src/main/java/com/umc/pediupatrao/config/DataInitializer.java`: bootstrap sem senha fixa, condicionado à ausência de ADMIN.
- `src/main/java/com/umc/pediupatrao/config/MongoInitConfig.java`: coleção de auditoria, índice único e compatibilidade com versões/perfis antigos.
- `src/main/java/com/umc/pediupatrao/config/SecurityConfig.java`: CSRF, segurança de métodos, matriz de rotas, resposta 401 nas APIs e filtro de atualização de perfil.
- `src/main/java/com/umc/pediupatrao/controller/ClienteController.java`: criação não aceita sobrescrever cliente por ID enviado no corpo.
- `src/main/java/com/umc/pediupatrao/controller/HomeController.java`: dados dos formulários de pedidos, criação segura de cliente e remoção da senha do modelo de edição de usuário.
- `src/main/java/com/umc/pediupatrao/controller/PedidoController.java`: DTO de criação e operações de desconto/cancelamento.
- `src/main/java/com/umc/pediupatrao/controller/ProdutoController.java`: criação sempre gera novo produto.
- `src/main/java/com/umc/pediupatrao/controller/UsuarioController.java`: atualização passa pela validação de perfis e senhas do serviço.
- `src/main/java/com/umc/pediupatrao/entity/Cliente.java`: versão para controle de concorrência.
- `src/main/java/com/umc/pediupatrao/entity/Pedido.java`: itens, preços, subtotal, desconto, total, estado tipado, versão, responsáveis, entrada, saída e justificativa.
- `src/main/java/com/umc/pediupatrao/entity/Usuario.java`: índice de login e senha não serializada nas respostas JSON.
- `src/main/java/com/umc/pediupatrao/service/ClienteService.java`: validação, autorização, transações e auditoria.
- `src/main/java/com/umc/pediupatrao/service/PedidoService.java`: cálculo, fluxo, bloqueios, transações, auditoria e autorização.
- `src/main/java/com/umc/pediupatrao/service/ProdutoService.java`: validação de preço, nome/situação e permissão de escrita.
- `src/main/java/com/umc/pediupatrao/service/UsuarioDetailsService.java`: autoridade efetivamente cadastrada e recusa de perfil inválido.
- `src/main/java/com/umc/pediupatrao/service/UsuarioService.java`: administração restrita e validação de cadastro, alteração e exclusão.
- `src/main/resources/application.properties`: configuração por ambiente e respostas de erro sem detalhes internos.
- `src/main/resources/templates/clientes/lista.html`: listagem Thymeleaf com escaping e ações por perfil; remove interpolação HTML de dados pessoais via JavaScript.
- `src/main/resources/templates/fragments/sidebar.html`: navegação por perfil e logout POST com CSRF.
- `src/main/resources/templates/layout.html`: metadados CSRF e carregamento do suporte a fetch.
- `src/main/resources/templates/pedidos.html`: cadastro com múltiplos itens, valores, histórico e ações por estado/perfil.
- `src/main/resources/templates/produtos/lista.html`: fragmento válido e cadastro, edição/exclusão compatíveis com a API JSON.
- `src/main/resources/templates/usuarios/form.html`: três perfis e senha vazia na edição.

## Todas as funções, métodos e interfaces adicionados

Os caminhos Java abaixo são relativos a `src/main/java/com/umc/pediupatrao/`. Métodos já existentes foram modificados conforme o inventário anterior; a lista abaixo identifica os novos, incluindo construtores explícitos e membros gerados relevantes.

| Arquivo | Novos métodos/construtores/interfaces |
| --- | --- |
| `config/MongoTransactionConfig.java` | `mongoCustomConversions()`, `transactionManager(MongoDatabaseFactory)` |
| `config/PerfilAtualFilter.java` | construtor `PerfilAtualFilter(UsuarioDetailsService)`, `doFilterInternal(HttpServletRequest, HttpServletResponse, FilterChain)` |
| `config/StatusPedidoReadConverter.java` | `convert(String)` |
| `controller/AuditoriaController.java` | construtor `AuditoriaController(AuditoriaService)`, `consultar(int)`, `pagina(int, Model)` |
| `controller/ErroControllerAdvice.java` | `requisicaoInvalida(Exception)`, `conflito()` |
| `controller/PedidoController.java` | construtor `PedidoController(PedidoService)`, `aplicarDesconto(String, Desconto)`, `cancelarPedido(String, Cancelamento)` |
| `entity/Cliente.java` | `getVersao()`, `setVersao(Long)` |
| `entity/StatusPedido.java` | `permiteAjustes()`, `permiteTransicao(StatusPedido)` |
| `repository/AuditoriaRepository.java` | nova interface `AuditoriaRepository`, métodos `insert(Auditoria)` e `findAll(Pageable)`; não expõe métodos de atualização/exclusão |
| `service/AuditoriaService.java` | construtor `AuditoriaService(AuditoriaRepository, OperadorService, ObjectMapper)`, `retrato(Object)`, `registrar(String, String, String, Map, Object, String)`, `consultar(int)` |
| `service/OperadorService.java` | `usuario()`, `perfil()` |
| `service/ClienteService.java` | construtor `ClienteService(ClienteRepository, PedidoRepository, AuditoriaService)` |
| `service/PedidoService.java` | construtor `PedidoService(PedidoRepository, ProdutoRepository, ClienteRepository, AuditoriaService, OperadorService)`, `buscar(String)`, `validarAjuste(Pedido)`, `aplicarDesconto(String, BigDecimal)`, `cancelarPedido(String, String)` |
| `service/UsuarioDetailsService.java` | `perfilAutorizado(String)` |
| `service/UsuarioService.java` | `validar(Usuario, String)`, `codificarSenha(String)`; construtor alterado para receber `OperadorService` |
| `src/main/resources/static/js/api.js` | `fetchComCsrf(recurso, configuracao)` substitui fetch adicionando CSRF apenas nas mutações da mesma origem |
| `src/main/resources/static/js/pedidos.js` | `executarPedido(caminho, configuracao)`, `adicionarItemPedido()`, callback nomeado `registrarPedido(submissao)`, `alterarStatusPedido(botao, status)`, `descontarPedido(botao)`, `cancelarPedido(botao)` |
| `src/main/resources/templates/produtos/lista.html` | `editarProduto(botao)`, `enviarProduto(caminho, configuracao)`, `excluirProduto(identificador)`, callback nomeado `salvarProduto(submissao)` |

Em `clientes/lista.html`, a função preexistente `excluirCliente` foi reescrita e `formatarEndereco`/`carregarClientes` foram substituídas pela renderização no servidor.

### Records e métodos gerados

Todos os records abaixo geram construtor canônico, `equals(Object)`, `hashCode()` e `toString()`, além dos acessores listados:

- `dto/NovoPedido.java`: record `NovoPedido` com `clienteId()`, `itens()`; record interno `ItemSolicitado` com `produtoId()`, `quantidade()`.
- `entity/ItemPedido.java`: record `ItemPedido` com `produtoId()`, `nome()`, `quantidade()`, `precoUnitario()`.
- `entity/Auditoria.java`: record `Auditoria` com `id()`, `usuario()`, `perfil()`, `dataHora()`, `operacao()`, `entidade()`, `entidadeId()`, `campos()`, `justificativa()`; record interno `Alteracao` com `anterior()`, `novo()`.
- `controller/PedidoController.java`: records `Desconto` com `percentual()` e `Cancelamento` com `justificativa()`.

Em `entity/Pedido.java`, Lombok preserva os acessores preexistentes e adiciona os pares completos: `getVersao`/`setVersao`, `getItens`/`setItens`, `getSubtotal`/`setSubtotal`, `getDescontoPercentual`/`setDescontoPercentual`, `getValorTotal`/`setValorTotal`, `getResponsavelRegistro`/`setResponsavelRegistro`, `getDataEntrada`/`setDataEntrada`, `getResponsavelSaida`/`setResponsavelSaida`, `getDataSaida`/`setDataSaida` e `getJustificativaCancelamento`/`setJustificativaCancelamento`. `getStatus`/`setStatus` passam a usar `StatusPedido`.

### Métodos de teste adicionados

Caminhos relativos a `src/test/java/com/umc/pediupatrao/`:

- `PedidoServiceTest.java`: `preparar()`, `calculaPrecoDoCatalogoERegistraEntrada()`, `rejeitaQuantidadesInvalidas()`, `exigeFluxoERegistraSaida()`, `descontoLimitadoESemAcumulo()`, `cancelamentoExigeJustificativaEBloqueiaAlteracoes()`, `validaTodasAsTransicoes()`.
- `SegurancaTest.java`: `preparar()`, `somenteGerenteAplicaDescontoInclusiveContraAdmin()`, `bloqueiaCancelamentoEStatusDeAtendente()`, `exigeCsrf()`, `protegeCadastroEAuditoria()`.
- `ClienteAuditoriaTest.java`: `registraTelefoneEEnderecoComAntesEDepois()`, `naoIncluiCredenciaisNaAuditoria()`, `impedeExclusaoDeClienteComHistorico()`.
- `TelasTest.java`: `preparar()`, `renderizaPedidosParaTodosOsPerfis()`, `renderizaProdutosClientesAuditoriaECadastro()`.

## Mocks e integrações

Mocks/stubs de repositórios, autenticação e serviços, e dados sintéticos, foram criados exclusivamente nos quatro arquivos de teste listados acima para validação isolada. Não são dados carregados na aplicação. Para validar a aplicação real, faltam `MONGODB_URI` e, quando não há ADMIN, `BOOTSTRAP_ADMIN_USERNAME`/`BOOTSTRAP_ADMIN_PASSWORD`. `MONGODB_DATABASE` é opcional. Os detalhes de configuração, migração e limitações estão no README.

## Implementação posterior — Dashboard AdminLTE

O inventário completo de novos arquivos, funções, métodos, records e interface desta etapa está em [docs/dashboard-walkthrough.md](docs/dashboard-walkthrough.md). Requisitos, APIs e evidências estão em [docs/dashboard.md](docs/dashboard.md). O resultado de 15 testes descrito acima corresponde à etapa anterior; consulte a evidência atualizada do dashboard para a suíte completa.
