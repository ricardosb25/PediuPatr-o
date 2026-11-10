package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.*;
import com.umc.pediupatrao.dto.DashboardResumo.*;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.DashboardRepository;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class DashboardService {
    private static final ZoneId FUSO_HORARIO = ZoneId.of("America/Sao_Paulo");
    private static final int MAXIMO_DIAS = 366;
    private final DashboardRepository repository;
    private final Clock clock;

    public DashboardService(DashboardRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public DashboardFiltros filtros() {
        LocalDate hoje = LocalDate.now(clock.withZone(FUSO_HORARIO));
        return new DashboardFiltros(hoje.minusDays(29), hoje, FUSO_HORARIO.getId(), MAXIMO_DIAS, 60,
                Arrays.stream(StatusPedido.values()).map(status -> new DashboardFiltros.OpcaoStatus(status.name(), rotulo(status))).toList());
    }

    public DashboardResumo consultar(LocalDate inicio, LocalDate fim, StatusPedido status) {
        DashboardFiltros padrao = filtros();
        LocalDate inicioEfetivo = inicio == null ? padrao.inicioPadrao() : inicio;
        LocalDate fimEfetivo = fim == null ? padrao.fimPadrao() : fim;
        validarPeriodo(inicioEfetivo, fimEfetivo);
        DashboardAgregado agregado = repository.consultar(inicioEfetivo.atStartOfDay(FUSO_HORARIO).toInstant(),
                fimEfetivo.plusDays(1).atStartOfDay(FUSO_HORARIO).toInstant(), status);
        var totais = agregado.totais();
        BigDecimal faturamento = moeda(totais.faturamentoLiquido());
        BigDecimal ticket = totais.pedidosComValor() == 0 ? moeda(BigDecimal.ZERO)
                : faturamento.divide(BigDecimal.valueOf(totais.pedidosComValor()), 2, RoundingMode.HALF_UP);
        BigDecimal taxa = totais.totalPedidos() == 0 ? moeda(BigDecimal.ZERO)
                : BigDecimal.valueOf(totais.pedidosCancelados()).multiply(new BigDecimal("100"))
                        .divide(BigDecimal.valueOf(totais.totalPedidos()), 2, RoundingMode.HALF_UP);
        Indicadores indicadores = new Indicadores(totais.totalPedidos(), totais.pedidosFinalizados(),
                totais.pedidosCancelados(), totais.pedidosEmAndamento(), totais.pedidosComValor(),
                totais.pedidosSemValor(), totais.pedidosSemItens(), faturamento, ticket, moeda(totais.descontosConcedidos()), taxa);
        Map<LocalDate, VendaDiaria> vendas = agregado.vendasPorDia().stream()
                .collect(Collectors.toMap(VendaDiaria::data, Function.identity()));
        List<VendaDiaria> serie = inicioEfetivo.datesUntil(fimEfetivo.plusDays(1))
                .map(data -> vendas.containsKey(data)
                        ? new VendaDiaria(data, vendas.get(data).pedidos(), moeda(vendas.get(data).faturamentoLiquido()))
                        : new VendaDiaria(data, 0, moeda(BigDecimal.ZERO))).toList();
        Map<String, Long> contagemStatus = agregado.situacaoPedidos().stream()
                .collect(Collectors.toMap(SituacaoPedido::status, SituacaoPedido::quantidade));
        List<SituacaoPedido> situacao = new ArrayList<>();
        for (StatusPedido estado : StatusPedido.values()) situacao.add(new SituacaoPedido(estado.name(), contagemStatus.getOrDefault(estado.name(), 0L)));
        if (contagemStatus.containsKey("NAO_INFORMADO")) situacao.add(new SituacaoPedido("NAO_INFORMADO", contagemStatus.get("NAO_INFORMADO")));
        List<String> avisos = new ArrayList<>();
        if (totais.pedidosSemValor() > 0) avisos.add(totais.pedidosSemValor() + " pedido(s) finalizado(s) sem total válido foram excluídos dos valores e do ticket médio.");
        if (totais.pedidosSemItens() > 0) avisos.add(totais.pedidosSemItens() + " pedido(s) finalizado(s) sem itens não contribuem para o ranking de produtos.");
        if (contagemStatus.containsKey("NAO_INFORMADO")) avisos.add("Há pedidos com status não reconhecido; confira a origem dos dados.");
        return new DashboardResumo(new FiltroAplicado(inicioEfetivo, fimEfetivo, status == null ? null : status.name(), FUSO_HORARIO.getId()),
                indicadores, serie, List.copyOf(situacao), agregado.produtosMaisVendidos(), agregado.pedidosRecentes(), List.copyOf(avisos), clock.instant());
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio.isBefore(LocalDate.of(2000, 1, 1)) || fim.isAfter(LocalDate.of(2100, 12, 31))
                || fim.isBefore(inicio) || ChronoUnit.DAYS.between(inicio, fim) + 1 > MAXIMO_DIAS) {
            throw new IllegalArgumentException("Informe início e fim entre 2000 e 2100, em ordem, com no máximo 366 dias inclusivos.");
        }
    }

    private BigDecimal moeda(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private String rotulo(StatusPedido status) {
        return switch (status) {
            case RECEBIDO -> "Recebido";
            case EM_PREPARACAO -> "Em preparação";
            case PRONTO -> "Pronto";
            case SAIU_PARA_ENTREGA -> "Saiu para entrega";
            case RETIRADO -> "Retirado";
            case FINALIZADO -> "Finalizado";
            case CANCELADO -> "Cancelado";
        };
    }
}
