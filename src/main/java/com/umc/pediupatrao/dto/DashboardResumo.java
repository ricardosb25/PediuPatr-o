package com.umc.pediupatrao.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DashboardResumo(
        FiltroAplicado filtro,
        Indicadores indicadores,
        List<VendaDiaria> vendasPorDia,
        List<SituacaoPedido> situacaoPedidos,
        List<ProdutoMaisVendido> produtosMaisVendidos,
        List<PedidoRecente> pedidosRecentes,
        List<String> avisos,
        Instant atualizadoEm) {

    public record FiltroAplicado(LocalDate inicio, LocalDate fim, String status, String fusoHorario) {}

    public record Indicadores(long totalPedidos, long pedidosFinalizados, long pedidosCancelados,
            long pedidosEmAndamento, long pedidosComValor, long pedidosSemValor,
            long pedidosSemItens, BigDecimal faturamentoLiquido, BigDecimal ticketMedio,
            BigDecimal descontosConcedidos, BigDecimal taxaCancelamento) {}

    public record VendaDiaria(LocalDate data, long pedidos, BigDecimal faturamentoLiquido) {}

    public record SituacaoPedido(String status, long quantidade) {}

    public record ProdutoMaisVendido(String produtoId, String nome, long quantidade, BigDecimal valorBruto) {}

    public record PedidoRecente(String id, String status, Instant dataEntrada, BigDecimal valorTotal) {}
}
