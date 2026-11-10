# Pediu Patrão - Sistema de Pizzaria

Projeto desenvolvido para a Atividade Prática M1 de Análise e Projeto de Software / Engenharia de Software.

O sistema controla o acesso de funcionários por perfil, faz o gerenciamento de pedidos de uma pizzaria e mantém um registro de auditoria no MongoDB.

---

Video da entrega do dashboard:

https://drive.google.com/file/d/1JJoGACJCCZjGOyLG5xYAC46EWbBRCt7S/view?usp=sharing

## Tecnologias Usadas

* Java 21
* Spring Boot 3
* Spring Security
* MongoDB
* Thymeleaf com Bootstrap 5

---

## Como Rodar o Projeto

### 1. Pré-requisitos
* Ter o Java 21 instalado na máquina.
* Ter o MongoDB rodando localmente (na porta 27017) ou uma string de conexão válida configurada em `src/main/resources/application.properties`:
  ```properties
  spring.data.mongodb.uri=mongodb://localhost:27017/pizzaria
  spring.data.mongodb.database=pizzaria
  ```

### 2. Rodar a aplicação
No terminal, dentro da pasta do projeto, execute:
```bash
./mvnw spring-boot:run
```
(ou `mvn spring-boot:run` se tiver o Maven instalado globalmente).

A aplicação vai subir no endereço: `http://localhost:8080`

---

## Usuários de Teste

O sistema já inicializa com 3 usuários padrão cadastrados para facilitar os testes de cada perfil:

| Usuário | Senha | Perfil | O que pode fazer |
| :--- | :--- | :--- | :--- |
| `admin` | `admin123` | ADMIN | Gerenciar usuários, ver pedidos e visualizar auditoria |
| `gerente` | `gerente123` | GERENTE | Cancelar pedidos, aplicar desconto, ver clientes e auditoria |
| `atendente` | `atendente123` | ATENDENTE | Cadastrar clientes, fazer pedidos e acompanhar andamento |

---

## O que foi implementado

### RF01 - Controle de Usuários e Permissões
* Criação dos 3 perfis: ADMIN, GERENTE e ATENDENTE.
* Correção no `UsuarioDetailsService` para carregar a role real do banco de dados (antes ficava fixo USER).
* Bloqueio no backend via Spring Security (`SecurityConfig` e `@PreAuthorize`).
* Apenas o ADMIN acessa a tela de usuários (`/usuarios`) e cria novos usuários com perfis.

### RF02 - Cadastro e Cálculo de Pedidos
* O pedido agora registra lista de itens, quantidades, valores e responsáveis.
* O cálculo do valor total é feito 100% no servidor (`PedidoService`), impedindo que valores sejam alterados pela requisição.
* Registra o usuário responsável pela criação e a data/hora de entrada.

### RF03 - Fluxo de Status do Pedido
* Fluxo de transições obrigatório:
  `RECEBIDO` -> `EM_PREPARACAO` -> `PRONTO` -> `SAIU_PARA_ENTREGA` (ou `RETIRADO`) -> `FINALIZADO`
* Bloqueio de transições inválidas (ex: tentar pular de RECEBIDO direto para FINALIZADO).
* Ao sair para entrega/retirada, registra automaticamente a data/hora de saída e quem despachou.
* Pedidos finalizados ou cancelados não podem ter o status alterado.

### RF04 - Cancelamento e Descontos (Exclusivo do Gerente)
* Somente o usuário com perfil GERENTE pode cancelar pedidos ou aplicar desconto (mesmo o ADMIN é bloqueado nessa regra).
* Cancelamento exige justificativa obrigatória e só é permitido antes da saída do pedido.
* O desconto é limitado a no máximo 20% do valor do pedido e só pode ser aplicado antes da saída.
* Ambas as ações geram log de auditoria.

### RF05 - Auditoria Persistente
* Criação da coleção `auditoria` no MongoDB.
* Registra automaticamente: usuário, perfil, data/hora, tipo de operação, entidade, valores anteriores e novos, e justificativas.
* Registra alterações cadastrais de clientes (mudança de telefone, endereço, etc.).
* Tela de consulta (`/auditoria`) acessível apenas por ADMIN e GERENTE.
* Não permite alteração nem exclusão de logs.

---

## Testes Automatizados

Para rodar os testes unitários do sistema:
```bash
./mvnw test
```
Foram implementados testes cobrindo cálculos de pedidos, validação de fluxo de transições, regras de cancelamento e desconto, e geração dos logs de auditoria.

---

## Roteiro Sugerido para o Vídeo de Demonstração

1. **Login como Atendente (`atendente` / `atendente123`)**:
   * Cadastrar um novo cliente ou alterar telefone/endereço de um cliente existente.
   * Criar um novo pedido em `/pedidos/novo`.
   * Avançar o status do pedido para `EM_PREPARACAO` e depois para `PRONTO`.
   * Tentar acessar `/usuarios` ou `/auditoria` para mostrar que o acesso é bloqueado (403).

2. **Login como Gerente (`gerente` / `gerente123`)**:
   * Abrir a tela de pedidos e aplicar um desconto de 10% no pedido.
   * Cancelar outro pedido informando a justificativa obrigatória.
   * Acessar a tela de `/auditoria` para mostrar os registros gerados com valores anteriores e novos.

3. **Login como Admin (`admin` / `admin123`)**:
   * Acessar a tela de `/usuarios` para mostrar a gestão de usuários e perfis.
   * Mostrar que o Admin pode consultar a auditoria, mas não pode aplicar desconto nem cancelar pedidos.
