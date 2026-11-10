package com.umc.pediupatrao;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.umc.pediupatrao.dto.DashboardAgregado;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.MongoDashboardRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.mongodb.core.MongoTemplate;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "DASHBOARD_TEST_MONGODB_URI", matches = ".+")
class MongoDashboardIntegrationTest {
    private MongoClient client;
    private MongoTemplate template;
    private MongoDashboardRepository repository;
    private String database;

    @BeforeEach
    void prepararBancoIsolado() {
        database = "pediupatrao_dashboard_test_" + UUID.randomUUID().toString().replace("-", "");
        client = MongoClients.create(System.getenv("DASHBOARD_TEST_MONGODB_URI"));
        template = new MongoTemplate(client, database);
        repository = new MongoDashboardRepository(template);
    }

    @AfterEach
    void removerSomenteBancoDoTeste() {
        try {
            if (template != null && database != null && database.matches("pediupatrao_dashboard_test_[a-f0-9]{32}")) template.getDb().drop();
        } finally {
            if (client != null) client.close();
        }
    }

    private Document pedido(String identificador, String entrada, String status, Object total) {
        Document documento = new Document("_id", identificador).append("status", status).append("valorTotal", total);
        if (entrada != null) documento.append("dataEntrada", Date.from(Instant.parse(entrada)));
        return documento;
    }

    @Test
    void agregaNoMongoRespeitandoPeriodoEstadosEValoresLegados() {
        List<Document> documentos = List.of(
                pedido("primeiro", "2026-11-05T03:00:00Z", "FINALIZADO", "90.00").append("subtotal", "100.00")
                        .append("itens", List.of(new Document("produtoId", "pizza").append("nome", "Calabresa")
                                .append("quantidade", 2).append("precoUnitario", "50.00"))),
                pedido("segundo", "2026-11-06T02:59:59Z", "ENTREGUE", new Decimal128(new BigDecimal("60.00"))).append("subtotal", "60.00")
                        .append("itens", List.of(new Document("produtoId", "pizza").append("nome", "Calabresa nova")
                                .append("quantidade", 1).append("precoUnitario", "60.00"))),
                pedido("cancelado", "2026-11-05T12:00:00Z", "CANCELADO", "999.00"),
                pedido("cozinha", "2026-11-05T12:00:00Z", "EM_PREPARO", "50.00"),
                pedido("sem-valor", "2026-11-05T12:00:00Z", "FINALIZADO", null),
                pedido("valor-nao-finito", "2026-11-05T12:00:00Z", "FINALIZADO", "NaN"),
                pedido("status-desconhecido", "2026-11-05T12:00:00Z", "ANTIGO", "40.00"),
                pedido("depois", "2026-11-06T03:00:00Z", "FINALIZADO", "1000.00"),
                pedido("antes", "2026-11-05T02:59:59Z", "FINALIZADO", "1000.00"),
                pedido("sem-data", null, "FINALIZADO", "1000.00"));
        template.getCollection("pedidos").insertMany(documentos);
        DashboardAgregado agregado = repository.consultar(Instant.parse("2026-11-05T03:00:00Z"), Instant.parse("2026-11-06T03:00:00Z"), null);
        assertEquals(7, agregado.totais().totalPedidos());
        assertEquals(4, agregado.totais().pedidosFinalizados());
        assertEquals(2, agregado.totais().pedidosComValor());
        assertEquals(2, agregado.totais().pedidosSemValor());
        assertEquals(2, agregado.totais().pedidosSemItens());
        assertEquals(0, new BigDecimal("150.00").compareTo(agregado.totais().faturamentoLiquido()));
        assertEquals(0, new BigDecimal("10.00").compareTo(agregado.totais().descontosConcedidos()));
        assertEquals(1, agregado.vendasPorDia().size());
        assertEquals(2, agregado.vendasPorDia().getFirst().pedidos());
        assertEquals(3, agregado.produtosMaisVendidos().getFirst().quantidade());
        assertEquals("Calabresa nova", agregado.produtosMaisVendidos().getFirst().nome());
        assertEquals(0, new BigDecimal("160.00").compareTo(agregado.produtosMaisVendidos().getFirst().valorBruto()));
    }

    @Test
    void statusFiltraTodasAsFacetasEAceitaAliasLegado() {
        template.getCollection("pedidos").insertMany(List.of(
                pedido("entregue", "2026-11-05T12:00:00Z", "ENTREGUE", "50.00"),
                pedido("cancelado", "2026-11-05T12:00:00Z", "CANCELADO", "80.00")));
        DashboardAgregado agregado = repository.consultar(Instant.parse("2026-11-05T03:00:00Z"), Instant.parse("2026-11-06T03:00:00Z"), StatusPedido.FINALIZADO);
        assertEquals(1, agregado.totais().totalPedidos());
        assertEquals(0, agregado.totais().pedidosCancelados());
        assertEquals("entregue", agregado.pedidosRecentes().getFirst().id());
    }

    @Test
    void colecaoVaziaRetornaZeros() {
        template.createCollection("pedidos");
        DashboardAgregado agregado = repository.consultar(Instant.parse("2026-11-05T03:00:00Z"), Instant.parse("2026-11-06T03:00:00Z"), null);
        assertEquals(0, agregado.totais().totalPedidos());
        assertTrue(agregado.vendasPorDia().isEmpty());
        assertTrue(agregado.produtosMaisVendidos().isEmpty());
    }
}
