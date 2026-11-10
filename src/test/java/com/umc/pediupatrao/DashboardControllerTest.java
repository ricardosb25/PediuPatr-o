package com.umc.pediupatrao;

import com.umc.pediupatrao.config.*;
import com.umc.pediupatrao.controller.DashboardController;
import com.umc.pediupatrao.dto.DashboardAgregado;
import com.umc.pediupatrao.dto.DashboardAgregado.Totais;
import com.umc.pediupatrao.repository.DashboardRepository;
import com.umc.pediupatrao.service.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, DashboardService.class, DashboardConfig.class})
class DashboardControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private DashboardRepository repository;
    @MockitoBean private UsuarioDetailsService usuarios;

    @BeforeEach
    void preparar() {
        for (String perfil : List.of("ADMIN", "GERENTE", "ATENDENTE")) {
            when(usuarios.loadUserByUsername(perfil)).thenReturn(User.withUsername(perfil).password("somente-teste").roles(perfil).build());
        }
        when(repository.consultar(any(), any(), any())).thenReturn(new DashboardAgregado(
                new Totais(0, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO), List.of(), List.of(), List.of(), List.of()));
    }

    @Test
    void adminEGerenteConsultamApiSemExporDadosPessoais() throws Exception {
        for (String perfil : List.of("ADMIN", "GERENTE")) {
            mvc.perform(get("/api/dashboard/resumo").param("inicio", "2026-11-01").param("fim", "2026-11-03")
                    .with(user(perfil).roles(perfil)))
                    .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.filtro.fusoHorario").value("America/Sao_Paulo"))
                    .andExpect(jsonPath("$.vendasPorDia.length()").value(3))
                    .andExpect(jsonPath("$.indicadores.faturamentoLiquido").value(0))
                    .andExpect(jsonPath("$.clientes").doesNotExist()).andExpect(jsonPath("$.usuarios").doesNotExist());
        }
    }

    @Test
    void filtrosExibemSeteEstadosELimites() throws Exception {
        mvc.perform(get("/api/dashboard/filtros").with(user("GERENTE").roles("GERENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status.length()").value(7))
                .andExpect(jsonPath("$.maximoDias").value(366)).andExpect(jsonPath("$.atualizacaoSegundos").value(60));
    }

    @Test
    void exigeAutenticacaoEBloqueiaAtendente() throws Exception {
        mvc.perform(get("/api/dashboard/resumo")).andExpect(status().isUnauthorized());
        for (String caminho : List.of("/dashboard", "/api/dashboard/resumo", "/api/dashboard/filtros")) {
            mvc.perform(get(caminho).with(user("ATENDENTE").roles("ATENDENTE"))).andExpect(status().isForbidden());
        }
        verifyNoInteractions(repository);
    }

    @Test
    void filtrosInvalidosRetornam400() throws Exception {
        for (String parametros : List.of("inicio=2026-02-30", "status=ENTREGUE", "inicio=2026-11-03&fim=2026-11-01", "inicio=2024-01-01&fim=2025-01-01")) {
            mvc.perform(get("/api/dashboard/resumo?" + parametros).with(user("ADMIN").roles("ADMIN")))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro").isString());
        }
        verifyNoInteractions(repository);
    }

    @Test
    void indisponibilidadeRetorna503SemDetalhesInternos() throws Exception {
        when(repository.consultar(any(), any(), any())).thenThrow(new DataAccessResourceFailureException("credencial-interna"));
        mvc.perform(get("/api/dashboard/resumo").with(user("ADMIN").roles("ADMIN")))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.erro").isString())
                .andExpect(content().string(not(containsString("credencial-interna"))));
    }

    @Test
    void geraPreviaVisualIsolada() throws Exception {
        java.nio.file.Path destino = java.nio.file.Path.of("target", "dashboard-preview");
        java.nio.file.Files.createDirectories(destino);
        LocalDate fim = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        LocalDate inicio = fim.minusDays(29);
        List<com.umc.pediupatrao.dto.DashboardResumo.VendaDiaria> vendas = inicio.datesUntil(fim.plusDays(1))
                .map(data -> new com.umc.pediupatrao.dto.DashboardResumo.VendaDiaria(data,
                        4 + data.getDayOfMonth() % 9,
                        new BigDecimal("57.90").multiply(BigDecimal.valueOf(4 + data.getDayOfMonth() % 9)))).toList();
        long finalizados = vendas.stream().mapToLong(com.umc.pediupatrao.dto.DashboardResumo.VendaDiaria::pedidos).sum();
        BigDecimal receita = vendas.stream().map(com.umc.pediupatrao.dto.DashboardResumo.VendaDiaria::faturamentoLiquido)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var agregado = new DashboardAgregado(new Totais(finalizados + 21, finalizados, 5, 16, finalizados, 0, 0, receita, new BigDecimal("256.00")), vendas,
                List.of(new com.umc.pediupatrao.dto.DashboardResumo.SituacaoPedido("FINALIZADO", finalizados),
                        new com.umc.pediupatrao.dto.DashboardResumo.SituacaoPedido("CANCELADO", 5),
                        new com.umc.pediupatrao.dto.DashboardResumo.SituacaoPedido("RECEBIDO", 4),
                        new com.umc.pediupatrao.dto.DashboardResumo.SituacaoPedido("EM_PREPARACAO", 6),
                        new com.umc.pediupatrao.dto.DashboardResumo.SituacaoPedido("PRONTO", 3),
                        new com.umc.pediupatrao.dto.DashboardResumo.SituacaoPedido("SAIU_PARA_ENTREGA", 2),
                        new com.umc.pediupatrao.dto.DashboardResumo.SituacaoPedido("RETIRADO", 1)),
                List.of(new com.umc.pediupatrao.dto.DashboardResumo.ProdutoMaisVendido("pizza-1", "Pizza Calabresa", 128, new BigDecimal("6393.60")),
                        new com.umc.pediupatrao.dto.DashboardResumo.ProdutoMaisVendido("pizza-2", "Pizza Margherita", 96, new BigDecimal("4795.20")),
                        new com.umc.pediupatrao.dto.DashboardResumo.ProdutoMaisVendido("bebida", "Refrigerante 2 litros", 83, new BigDecimal("1245.00")),
                        new com.umc.pediupatrao.dto.DashboardResumo.ProdutoMaisVendido("pizza-3", "Pizza Portuguesa", 64, new BigDecimal("3833.60")),
                        new com.umc.pediupatrao.dto.DashboardResumo.ProdutoMaisVendido("lanche", "Cheeseburger da casa", 42, new BigDecimal("1255.80"))),
                List.of(new com.umc.pediupatrao.dto.DashboardResumo.PedidoRecente("teste-00001328", "EM_PREPARACAO", Instant.now(), new BigDecimal("89.90")),
                        new com.umc.pediupatrao.dto.DashboardResumo.PedidoRecente("teste-00001327", "PRONTO", Instant.now().minusSeconds(120), new BigDecimal("124.80")),
                        new com.umc.pediupatrao.dto.DashboardResumo.PedidoRecente("teste-00001326", "FINALIZADO", Instant.now().minusSeconds(300), new BigDecimal("57.90")),
                        new com.umc.pediupatrao.dto.DashboardResumo.PedidoRecente("teste-00001325", "CANCELADO", Instant.now().minusSeconds(480), new BigDecimal("59.90"))));
        when(repository.consultar(any(), any(), any())).thenReturn(agregado);
        for (String pagina : List.of("dashboard", "api/dashboard/filtros", "api/dashboard/resumo")) {
            var resposta = mvc.perform(get("/" + pagina).with(user("ADMIN").roles("ADMIN")))
                    .andExpect(status().isOk()).andReturn().getResponse();
            String arquivo = pagina.equals("dashboard") ? "dashboard.html" : pagina.endsWith("filtros") ? "filtros.json" : "resumo.json";
            java.nio.file.Files.writeString(destino.resolve(arquivo), resposta.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        }
        when(repository.consultar(any(), any(), any())).thenReturn(new DashboardAgregado(
                new Totais(0, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO), List.of(), List.of(), List.of(), List.of()));
        var vazio = mvc.perform(get("/api/dashboard/resumo").with(user("ADMIN").roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse();
        java.nio.file.Files.writeString(destino.resolve("resumo-vazio.json"), vazio.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void paginaUsaAdminLteEAssetsLocais() throws Exception {
        mvc.perform(get("/dashboard").with(user("ADMIN").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("/dist/css/adminlte.css")))
                .andExpect(content().string(containsString("/plugins/chart.js/Chart.min.js")))
                .andExpect(content().string(containsString("id=\"grafico-vendas\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }
}
