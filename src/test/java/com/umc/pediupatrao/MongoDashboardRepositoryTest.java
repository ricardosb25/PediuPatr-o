package com.umc.pediupatrao;

import com.umc.pediupatrao.dto.DashboardAgregado;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.MongoDashboardRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MongoDashboardRepositoryTest {
    @Test
    void enviaPeriodoEStatusAoMongoEMapeiaDecimais() {
        MongoTemplate template = mock(MongoTemplate.class);
        Document resultado = new Document("totais", List.of(new Document("totalPedidos", 2L)
                .append("pedidosFinalizados", 2).append("pedidosComValor", 2)
                .append("faturamentoLiquido", new Decimal128(new BigDecimal("89.90")))))
                .append("vendasPorDia", List.of(new Document("_id", "2026-11-05").append("pedidos", 2)
                        .append("faturamentoLiquido", new Decimal128(new BigDecimal("89.90")))))
                .append("situacaoPedidos", List.of(new Document("_id", "FINALIZADO").append("quantidade", 2)))
                .append("produtosMaisVendidos", List.of(new Document("_id", "pizza").append("nome", "Pizza")
                        .append("quantidade", new Decimal128(new BigDecimal("3"))).append("valorBruto", new Decimal128(new BigDecimal("99.90")))))
                .append("pedidosRecentes", List.of(new Document("_id", "pedido").append("status", "FINALIZADO")
                        .append("dataEntrada", Date.from(Instant.parse("2026-11-05T12:00:00Z"))).append("valorTotal", new Decimal128(new BigDecimal("89.90")))));
        when(template.aggregate(any(Aggregation.class), eq("pedidos"), eq(Document.class)))
                .thenReturn(new AggregationResults<>(List.of(resultado), new Document()));
        Instant inicio = Instant.parse("2026-11-05T03:00:00Z");
        Instant fim = Instant.parse("2026-11-06T03:00:00Z");
        DashboardAgregado agregado = new MongoDashboardRepository(template).consultar(inicio, fim, StatusPedido.FINALIZADO);
        assertEquals(new BigDecimal("89.90"), agregado.totais().faturamentoLiquido());
        assertEquals(3, agregado.produtosMaisVendidos().getFirst().quantidade());
        assertEquals("pedido", agregado.pedidosRecentes().getFirst().id());
        ArgumentCaptor<Aggregation> captor = ArgumentCaptor.forClass(Aggregation.class);
        verify(template).aggregate(captor.capture(), eq("pedidos"), eq(Document.class));
        List<Document> pipeline = captor.getValue().toPipeline(Aggregation.DEFAULT_CONTEXT);
        Document filtro = pipeline.getFirst().get("$match", Document.class);
        assertEquals(new Document("$gte", Date.from(inicio)).append("$lt", Date.from(fim)), filtro.get("dataEntrada"));
        assertEquals(List.of("FINALIZADO", "ENTREGUE"), filtro.get("status", Document.class).get("$in"));
        assertEquals(Set.of("totais", "vendasPorDia", "situacaoPedidos", "produtosMaisVendidos", "pedidosRecentes"), pipeline.getLast().get("$facet", Document.class).keySet());
    }

    @Test
    void aceitaResultadoVazioSemEncontrarDocumentos() {
        MongoTemplate template = mock(MongoTemplate.class);
        when(template.aggregate(any(Aggregation.class), eq("pedidos"), eq(Document.class)))
                .thenReturn(new AggregationResults<>(List.of(), new Document()));
        DashboardAgregado agregado = new MongoDashboardRepository(template).consultar(Instant.EPOCH, Instant.now(), null);
        assertEquals(0, agregado.totais().totalPedidos());
        assertEquals(BigDecimal.ZERO, agregado.totais().faturamentoLiquido());
        assertTrue(agregado.pedidosRecentes().isEmpty());
    }
}
