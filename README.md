# PediuPatrão

Sistema de pizzaria em Java 21, Spring Boot 3.4.4, Spring Security, Thymeleaf e MongoDB. Implementa RF01 a RF05 da Atividade Prática M1, Parte 2.

## Executar

Requisitos: JDK 21, Maven 3.9 e MongoDB Atlas ou MongoDB 4.0+ com replica set. MongoDB standalone não suporta as transações usadas para gravar operações e auditoria de forma atômica. A conta do banco precisa criar coleções e índices e executar transações.

Na raiz do projeto, copie o `.env.example` se ainda não tiver um `.env`.

```powershell
Copy-Item .env.example .env
```

Preencha `MONGODB_URI` com a URI completa do Atlas ou do replica set. Use o formato `CHAVE=valor`, sem aspas. O Spring Boot carrega o `.env` ao iniciar na raiz do projeto. O arquivo está no `.gitignore`. Variáveis de ambiente do terminal têm prioridade sobre os valores do arquivo.

No primeiro acesso, preencha também `BOOTSTRAP_ADMIN_USERNAME` e `BOOTSTRAP_ADMIN_PASSWORD` no `.env`. Depois, inicie a aplicação na mesma pasta.

```powershell
mvn spring-boot:run
```

Ajuste `JAVA_HOME` ao local do seu JDK se necessário. Acesse [a tela de login](http://localhost:8080/login). `MONGODB_DATABASE` é opcional e assume `pizzaria`. As chaves `BOOTSTRAP_ADMIN_*` são necessárias apenas quando o banco ainda não tem um ADMIN. Escolha um login novo e uma senha de pelo menos 12 caracteres e no máximo 72 bytes UTF-8. Após criar o ADMIN, você pode limpar essas duas chaves do `.env`.

Não foram executadas conexões com o banco anteriormente configurado. Credenciais foram retiradas de application.properties. Antes de usar um banco existente, faça backup: a inicialização adiciona a versão 0 aos clientes/pedidos antigos e converte perfis USER ou ausentes para ATENDENTE, sem promover contas a ADMIN. Um índice único bloqueia logins duplicados; duplicidades existentes devem ser resolvidas antes de iniciar. EM_PREPARO/EM PREPARAÇÃO são lidos como EM_PREPARACAO; ENTREGUE como FINALIZADO; SAIU PARA ENTREGA como SAIU_PARA_ENTREGA. Outros estados legados desconhecidos precisam de revisão explícita do responsável pelo banco. Dados históricos ausentes não são inventados. Pedidos legados sem subtotal não aceitam desconto.

## Permissões

| Operação | ADMIN | GERENTE | ATENDENTE |
| --- | --- | --- | --- |
| Administrar usuários e atribuir perfis | Sim | Não | Não |
| Consultar pedidos e catálogo | Sim | Sim | Sim |
| Cadastrar/editar/excluir produtos | Sim | Não | Não |
| Registrar pedidos | Não | Sim | Sim |
| Alterar status/registrar saída | Não | Sim | Não |
| Desconto e cancelamento | Não | Sim | Não |
| Consultar clientes | Não | Sim | Sim |
| Cadastrar/editar/excluir clientes | Não | Não | Sim |
| Consultar auditoria | Sim | Sim | Não |

ADMIN não herda as permissões de GERENTE. Um administrador não pode excluir a própria conta nem alterar o próprio login/perfil. Alterações de perfil e exclusões passam a valer na próxima requisição de sessões já abertas. Só ADMIN pode cadastrar usuários; perfis inválidos são rejeitados no serviço. Clientes com histórico de pedidos não podem ser excluídos.

## Pedidos e auditoria

A tela Pedidos permite vários produtos e quantidades. O servidor aceita somente cliente, IDs dos produtos e quantidades na criação; usa preços de produtos ativos do catálogo e preserva uma cópia do nome e preço unitário no pedido. Total, desconto, estado, horários e responsáveis são definidos no servidor. Dinheiro é calculado em BigDecimal com duas casas e HALF_UP. Descontos entre 0 e 20% substituem o percentual anterior e nunca se acumulam.

Fluxo obrigatório: RECEBIDO → EM_PREPARACAO → PRONTO → SAIU_PARA_ENTREGA ou RETIRADO → FINALIZADO. A saída registra instante e responsável. GERENTE pode cancelar com justificativa não vazia de até 1000 caracteres ou aplicar desconto somente nos três primeiros estados. Cancelamento encerra qualquer modificação. Versões impedem gravações concorrentes silenciosas; conflito retorna HTTP 409.

Auditoria registra usuário, perfil, instante UTC, operação, entidade/ID, campos anteriores e novos e justificativa. Inclui criação de pedidos, status, descontos, cancelamentos, criação/alteração/exclusão de clientes. É persistida em coleção própria na mesma transação da operação; falha na auditoria impede a confirmação da alteração. Não há endpoints de alteração ou exclusão de auditoria. A consulta é paginada em 50 registros, restrita a ADMIN/GERENTE; dados de telefone/endereço são exibidos apenas para conferência dessas operações. Não registra credenciais. Datas são exibidas em UTC (sufixo Z).

## APIs e CSRF

Login usa sessão e formulários Thymeleaf incluem token CSRF. Fetch de mesma origem recebe o token dos metadados do layout. Não desative CSRF em APIs autenticadas por cookie.

- GET/POST `/api/pedidos`: consulta/criação. Criação recebe objeto com `clienteId` e lista `itens`; cada item tem `produtoId` e `quantidade`.
- PUT `/api/pedidos/{id}/status?status=EM_PREPARACAO`: transição.
- PUT `/api/pedidos/{id}/desconto`: corpo JSON com `percentual` numérico.
- PUT `/api/pedidos/{id}/cancelamento`: corpo JSON com `justificativa` textual.
- GET `/api/auditoria?pagina=0`: consulta de auditoria.
- `/api/clientes`, `/api/produtos`, `/api/usuarios`: CRUD sujeito à matriz acima.

Use a sessão do usuário e o cabeçalho cujo nome está no meta `_csrf_header`, com valor do meta `_csrf`. Sem autenticação APIs retornam 401; sem permissão ou CSRF válido, 403; dados inválidos retornam 400; IDs inexistentes, 404.

## Testes e demonstração

```powershell
mvn test
```

A suíte padrão pode executar sem MongoDB. Os testes originais usam mocks e dados sintéticos em `src/test/java/com/umc/pediupatrao/PedidoServiceTest.java`, `SegurancaTest.java`, `ClienteAuditoriaTest.java` e `TelasTest.java`, para validar serviços, autorização HTTP e renderização sem tocar em dados reais. Os testes adicionais do dashboard e sua prévia isolada estão descritos abaixo e em `docs/dashboard.md`. Não há carga fictícia na aplicação. Os testes sem conexão não comprovam a integração de transações com um MongoDB real.

Roteiro do vídeo individual:

1. Entrar com ADMIN, cadastrar GERENTE e ATENDENTE e cadastrar produtos ativos.
2. Entrar com ATENDENTE, cadastrar cliente e registrar pedido com mais de um item. Mostrar entrada e responsável.
3. Ainda com ATENDENTE, tentar desconto, cancelamento e consulta à auditoria nas APIs usando CSRF válido; mostrar 403. Tentar cadastro de usuários e alteração de perfil; mostrar bloqueio.
4. Entrar com ADMIN e tentar desconto/cancelamento com CSRF válido; mostrar 403.
5. Entrar com GERENTE, aplicar 20% e mostrar o total. Tentar 20,01% e mostrar 400.
6. Mostrar tentativa de pular estados, depois executar o fluxo até saída e finalização; conferir horário e responsável. Tentar desconto/cancelamento após saída.
7. Criar outro pedido e cancelar com justificativa; demonstrar bloqueio de mudanças posteriores.
8. Alterar telefone e endereço do cliente com ATENDENTE. Com GERENTE e ADMIN, conferir auditoria com valores anteriores/novos e justificativas.
9. Tentar uma mutação sem CSRF e mostrar 403.

A gravação individual e a publicação no GitHub devem ser feitas pelo aluno; este projeto não inclui um vídeo de execução nem operações Git.

O inventário de arquivos e métodos está em [WALKTHROUGH.md](WALKTHROUGH.md).

## Dashboard administrativo (AdminLTE)

Acesse `/dashboard` com ADMIN ou GERENTE, ou use o link Dashboard no menu. O painel usa AdminLTE 3.2 e Chart.js locais para exibir faturamento, ticket, pedidos, gráficos diários/status, ranking de produtos e pedidos recentes. Oferece filtros por período/status e atualização automática a cada 60 segundos.

As APIs de consulta são `GET /api/dashboard/filtros` e `GET /api/dashboard/resumo`. Os dados vêm de agregação da coleção de pedidos no MongoDB; não há carga de demonstração no aplicativo. Faturamento e ticket usam somente finalizados com valor válido, agrupados pela entrada do pedido no fuso de São Paulo.

- [Requisitos, endpoints, decisões e testes](docs/dashboard.md)
- [Inventário dos arquivos e métodos do dashboard](docs/dashboard-walkthrough.md)
- [Evidências de testes](docs/evidencias/testes-maven.txt)

Para validar a agregação em MongoDB real, configure `DASHBOARD_TEST_MONGODB_URI` apontando para um ambiente de testes e execute `mvn '-Dtest=MongoDashboardIntegrationTest' test`. Esses testes criam um banco isolado e descartável; sem a variável ficam desativados. A prévia visual, os mocks e os dados sintéticos existem apenas nos testes e em `target/dashboard-preview`; não substituem a execução da aplicação com `MONGODB_URI`.
