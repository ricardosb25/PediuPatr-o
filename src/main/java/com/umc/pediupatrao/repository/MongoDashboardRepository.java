package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.dto.DashboardAgregado;
import com.umc.pediupatrao.dto.DashboardAgregado.Totais;
import com.umc.pediupatrao.dto.DashboardResumo.*;
import com.umc.pediupatrao.entity.StatusPedido;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.stereotype.Repository;

@Repository
public class MongoDashboardRepository implements DashboardRepository {
    private static final String FUSO_HORARIO = "America/Sao_Paulo";
    private final MongoTemplate mongoTemplate;

    public MongoDashboardRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public DashboardAgregado consultar(Instant inicio, Instant fimExclusivo, StatusPedido status) {
        Document periodo = new Document("dataEntrada", new Document("$gte", Date.from(inicio))
                .append("$lt", Date.from(fimExclusivo)));
        if (status != null) periodo.append("status", new Document("$in", valoresPersistidos(status)));

        Document normalizacao = new Document("statusNormalizado", statusNormalizado())
                .append("totalNumerico", decimal("$valorTotal"))
                .append("subtotalNumerico", decimal("$subtotal"));
        Document vendaValida = new Document("$and", List.of(
                new Document("$eq", List.of("$statusNormalizado", "FINALIZADO")),
                new Document("$ne", Arrays.asList("$totalNumerico", null)),
                new Document("$gte", List.of("$totalNumerico", 0))));
        Document facetas = new Document("totais", totais())
                .append("vendasPorDia", vendasPorDia())
                .append("situacaoPedidos", List.of(new Document("$group", new Document("_id", "$statusNormalizado")
                        .append("quantidade", new Document("$sum", 1))), new Document("$sort", new Document("_id", 1))))
                .append("produtosMaisVendidos", produtosMaisVendidos())
                .append("pedidosRecentes", List.of(new Document("$sort", new Document("dataEntrada", -1).append("_id", -1)),
                        new Document("$limit", 8), new Document("$project", new Document("status", "$statusNormalizado")
                                .append("dataEntrada", 1).append("valorTotal", new Document("$cond", Arrays.asList(
                                        new Document("$gte", List.of("$totalNumerico", 0)), "$totalNumerico", null))))));
        List<AggregationOperation> operacoes = List.of(
                contexto -> new Document("$match", periodo),
                contexto -> new Document("$addFields", normalizacao),
                contexto -> new Document("$addFields", new Document("vendaValida", vendaValida)),
                contexto -> new Document("$facet", facetas));
        Aggregation agregacao = Aggregation.newAggregation(operacoes)
                .withOptions(AggregationOptions.builder().allowDiskUse(true).maxTime(Duration.ofSeconds(10)).build());
        Document resultado = mongoTemplate.aggregate(agregacao, "pedidos", Document.class).getUniqueMappedResult();
        return mapear(resultado == null ? new Document() : resultado);
    }

    private List<String> valoresPersistidos(StatusPedido status) {
        return switch (status) {
            case EM_PREPARACAO -> List.of("EM_PREPARACAO", "EM_PREPARO", "EM PREPARAÇÃO");
            case FINALIZADO -> List.of("FINALIZADO", "ENTREGUE");
            case SAIU_PARA_ENTREGA -> List.of("SAIU_PARA_ENTREGA", "SAIU PARA ENTREGA");
            default -> List.of(status.name());
        };
    }

    private Document statusNormalizado() {
        List<Document> alternativas = new ArrayList<>();
        for (StatusPedido status : StatusPedido.values()) {
            alternativas.add(new Document("case", new Document("$in", List.of("$status", valoresPersistidos(status))))
                    .append("then", status.name()));
        }
        return new Document("$switch", new Document("branches", alternativas).append("default", "NAO_INFORMADO"));
    }

    private Document decimal(String campo) {
        Document conversao = new Document("$convert", new Document("input", campo).append("to", "decimal")
                .append("onError", null).append("onNull", null));
        Document naoFinito = new Document("$in", List.of(new Document("$toString", "$$numero"),
                List.of("NaN", "Infinity", "-Infinity")));
        return new Document("$let", new Document("vars", new Document("numero", conversao))
                .append("in", new Document("$cond", Arrays.asList(naoFinito, null, "$$numero"))));
    }

    private Document somaCondicional(Object condicao, Object valor) {
        return new Document("$sum", new Document("$cond", List.of(condicao, valor, 0)));
    }

    private List<Document> totais() {
        Document finalizado = new Document("$eq", List.of("$statusNormalizado", "FINALIZADO"));
        Document cancelado = new Document("$eq", List.of("$statusNormalizado", "CANCELADO"));
        Document andamento = new Document("$in", List.of("$statusNormalizado",
                List.of("RECEBIDO", "EM_PREPARACAO", "PRONTO", "SAIU_PARA_ENTREGA", "RETIRADO")));
        Document tamanhoItens = new Document("$cond", List.of(new Document("$isArray", "$itens"),
                new Document("$size", "$itens"), 0));
        Document grupo = new Document("_id", null)
                .append("totalPedidos", new Document("$sum", 1))
                .append("pedidosFinalizados", somaCondicional(finalizado, 1))
                .append("pedidosCancelados", somaCondicional(cancelado, 1))
                .append("pedidosEmAndamento", somaCondicional(andamento, 1))
                .append("pedidosComValor", somaCondicional("$vendaValida", 1))
                .append("pedidosSemValor", somaCondicional(new Document("$and", List.of(finalizado,
                        new Document("$eq", List.of("$vendaValida", false)))), 1))
                .append("pedidosSemItens", somaCondicional(new Document("$and", List.of(finalizado,
                        new Document("$eq", List.of(tamanhoItens, 0)))), 1))
                .append("faturamentoLiquido", somaCondicional("$vendaValida", "$totalNumerico"))
                .append("descontosConcedidos", somaCondicional(new Document("$and", List.of("$vendaValida",
                        new Document("$gte", List.of("$subtotalNumerico", "$totalNumerico")))),
                        new Document("$subtract", List.of("$subtotalNumerico", "$totalNumerico"))));
        return List.of(new Document("$group", grupo));
    }

    private List<Document> vendasPorDia() {
        return List.of(new Document("$match", new Document("vendaValida", true)),
                new Document("$group", new Document("_id", new Document("$dateToString",
                        new Document("format", "%Y-%m-%d").append("date", "$dataEntrada").append("timezone", FUSO_HORARIO)))
                        .append("pedidos", new Document("$sum", 1))
                        .append("faturamentoLiquido", new Document("$sum", "$totalNumerico"))),
                new Document("$sort", new Document("_id", 1)));
    }

    private List<Document> produtosMaisVendidos() {
        return List.of(new Document("$match", new Document("statusNormalizado", "FINALIZADO")
                        .append("$expr", new Document("$isArray", "$itens"))),
                new Document("$sort", new Document("dataEntrada", -1).append("_id", -1)),
                new Document("$unwind", "$itens"),
                new Document("$addFields", new Document("quantidadeItem", decimal("$itens.quantidade"))
                        .append("precoItem", decimal("$itens.precoUnitario"))),
                new Document("$match", new Document("itens.produtoId", new Document("$type", "string").append("$ne", ""))
                        .append("quantidadeItem", new Document("$gt", 0))
                        .append("$expr", new Document("$eq", List.of(new Document("$mod", List.of("$quantidadeItem", 1)), 0)))),
                new Document("$group", new Document("_id", "$itens.produtoId")
                        .append("nome", new Document("$first", new Document("$cond", List.of(
                                new Document("$eq", List.of(new Document("$type", "$itens.nome"), "string")),
                                "$itens.nome", "Produto sem nome"))))
                        .append("quantidade", new Document("$sum", "$quantidadeItem"))
                        .append("valorBruto", somaCondicional(new Document("$gte", List.of("$precoItem", 0)),
                                new Document("$multiply", List.of("$quantidadeItem", "$precoItem"))))),
                new Document("$sort", new Document("quantidade", -1).append("valorBruto", -1).append("_id", 1)),
                new Document("$limit", 10));
    }

    private DashboardAgregado mapear(Document resultado) {
        Document totais = documentos(resultado, "totais").stream().findFirst().orElse(new Document());
        Totais resumo = new Totais(numero(totais, "totalPedidos"), numero(totais, "pedidosFinalizados"),
                numero(totais, "pedidosCancelados"), numero(totais, "pedidosEmAndamento"),
                numero(totais, "pedidosComValor"), numero(totais, "pedidosSemValor"), numero(totais, "pedidosSemItens"),
                dinheiro(totais.get("faturamentoLiquido")), dinheiro(totais.get("descontosConcedidos")));
        return new DashboardAgregado(resumo,
                documentos(resultado, "vendasPorDia").stream().map(linha -> new VendaDiaria(LocalDate.parse(linha.getString("_id")),
                        numero(linha, "pedidos"), dinheiro(linha.get("faturamentoLiquido")))).toList(),
                documentos(resultado, "situacaoPedidos").stream().map(linha -> new SituacaoPedido(linha.getString("_id"), numero(linha, "quantidade"))).toList(),
                documentos(resultado, "produtosMaisVendidos").stream().map(linha -> new ProdutoMaisVendido(linha.getString("_id"),
                        linha.getString("nome"), numero(linha, "quantidade"), dinheiro(linha.get("valorBruto")))).toList(),
                documentos(resultado, "pedidosRecentes").stream().map(linha -> new PedidoRecente(linha.get("_id").toString(),
                        linha.getString("status"), linha.getDate("dataEntrada").toInstant(),
                        linha.get("valorTotal") == null ? null : dinheiro(linha.get("valorTotal")))).toList());
    }

    private List<Document> documentos(Document resultado, String campo) {
        return resultado.getList(campo, Document.class, List.of());
    }

    private long numero(Document documento, String campo) {
        Object valor = documento.get(campo);
        return valor == null ? 0 : dinheiro(valor).longValue();
    }

    private BigDecimal dinheiro(Object valor) {
        if (valor == null) return BigDecimal.ZERO;
        return valor instanceof Decimal128 decimal ? decimal.bigDecimalValue() : new BigDecimal(valor.toString());
    }
}
