package com.umc.pediupatrao.dto;

import java.math.BigDecimal;
import java.util.List;
import com.umc.pediupatrao.dto.DashboardResumo.*;

public record DashboardAgregado(Totais totais, List<VendaDiaria> vendasPorDia,
        List<SituacaoPedido> situacaoPedidos, List<ProdutoMaisVendido> produtosMaisVendidos,
        List<PedidoRecente> pedidosRecentes) {
    public record Totais(long totalPedidos, long pedidosFinalizados, long pedidosCancelados,
            long pedidosEmAndamento, long pedidosComValor, long pedidosSemValor,
            long pedidosSemItens, BigDecimal faturamentoLiquido, BigDecimal descontosConcedidos) {}
}
