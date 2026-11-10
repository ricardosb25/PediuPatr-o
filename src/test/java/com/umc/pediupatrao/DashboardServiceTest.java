package com.umc.pediupatrao;

import com.umc.pediupatrao.dto.*;
import com.umc.pediupatrao.dto.DashboardAgregado.Totais;
import com.umc.pediupatrao.dto.DashboardResumo.*;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.DashboardRepository;
import com.umc.pediupatrao.service.DashboardService;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardServiceTest {
    private DashboardRepository repository;
    private DashboardService service;

    @BeforeEach
    void preparar() {
        repository = mock(DashboardRepository.class);
        service = new DashboardService(repository, Clock.fixed(Instant.parse("2026-11-06T01:00:00Z"), ZoneOffset.UTC));
        when(repository.consultar(any(), any(), any())).thenReturn(agregadoVazio());
    }

    private DashboardAgregado agregadoVazio() {
        return new DashboardAgregado(new Totais(0, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO),
                List.of(), List.of(), List.of(), List.of());
    }

    @Test
    void usaTrintaDiasEFusoDeSaoPaulo() {
        DashboardFiltros filtros = service.filtros();
        assertEquals(LocalDate.of(2026, 11, 5), filtros.fimPadrao());
        assertEquals(LocalDate.of(2026, 10, 7), filtros.inicioPadrao());
        assertEquals(7, filtros.status().size());
        assertEquals("America/Sao_Paulo", filtros.fusoHorario());
        service.consultar(null, null, null);
        verify(repository).consultar(Instant.parse("2026-10-07T03:00:00Z"), Instant.parse("2026-11-06T03:00:00Z"), null);
    }

    @Test
    void calculaTicketTaxaEPrencheDiasSemVenda() {
        Totais totais = new Totais(10, 3, 2, 5, 3, 0, 0, new BigDecimal("200.00"), new BigDecimal("20.00"));
        when(repository.consultar(any(), any(), any())).thenReturn(new DashboardAgregado(totais,
                List.of(new VendaDiaria(LocalDate.of(2026, 11, 2), 3, new BigDecimal("200.00"))), List.of(), List.of(), List.of()));
        DashboardResumo resumo = service.consultar(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), null);
        assertEquals(new BigDecimal("66.67"), resumo.indicadores().ticketMedio());
        assertEquals(new BigDecimal("20.00"), resumo.indicadores().taxaCancelamento());
        assertEquals(3, resumo.vendasPorDia().size());
        assertEquals(new BigDecimal("0.00"), resumo.vendasPorDia().getFirst().faturamentoLiquido());
        assertEquals(3, resumo.vendasPorDia().get(1).pedidos());
        assertEquals(7, resumo.situacaoPedidos().size());
    }

    @Test
    void evitaDivisaoPorZeroSemInventarPedidos() {
        DashboardResumo resumo = service.consultar(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1), null);
        assertEquals(new BigDecimal("0.00"), resumo.indicadores().ticketMedio());
        assertEquals(new BigDecimal("0.00"), resumo.indicadores().taxaCancelamento());
        assertTrue(resumo.pedidosRecentes().isEmpty());
        assertTrue(resumo.produtosMaisVendidos().isEmpty());
    }

    @Test
    void sinalizaLegadosEStatusNaoReconhecidos() {
        Totais totais = new Totais(3, 2, 0, 0, 1, 1, 2, new BigDecimal("15"), BigDecimal.ZERO);
        when(repository.consultar(any(), any(), any())).thenReturn(new DashboardAgregado(totais, List.of(),
                List.of(new SituacaoPedido("NAO_INFORMADO", 1)), List.of(), List.of()));
        DashboardResumo resumo = service.consultar(null, null, null);
        assertEquals(3, resumo.avisos().size());
        assertEquals(new BigDecimal("15.00"), resumo.indicadores().ticketMedio());
        assertEquals(8, resumo.situacaoPedidos().size());
    }

    @Test
    void rejeitaIntervalosInvalidosAntesDeConsultarBanco() {
        for (LocalDate[] intervalo : List.of(
                new LocalDate[]{LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 4)},
                new LocalDate[]{LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1)},
                new LocalDate[]{LocalDate.of(1999, 12, 31), LocalDate.of(2000, 1, 1)},
                new LocalDate[]{LocalDate.of(2100, 12, 31), LocalDate.of(2101, 1, 1)})) {
            assertThrows(IllegalArgumentException.class, () -> service.consultar(intervalo[0], intervalo[1], null));
        }
        verifyNoInteractions(repository);
    }

    @Test
    void aceitaAnoBissextoEEncaminhaStatus() {
        DashboardResumo resumo = service.consultar(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), StatusPedido.CANCELADO);
        assertEquals(366, resumo.vendasPorDia().size());
        assertEquals("CANCELADO", resumo.filtro().status());
        verify(repository).consultar(Instant.parse("2024-01-01T03:00:00Z"), Instant.parse("2025-01-01T03:00:00Z"), StatusPedido.CANCELADO);
    }
}
