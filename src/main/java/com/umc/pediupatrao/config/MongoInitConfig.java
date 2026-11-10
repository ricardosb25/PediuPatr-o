package com.umc.pediupatrao.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class MongoInitConfig implements CommandLineRunner {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) throws Exception {
        if (!mongoTemplate.collectionExists("auditoria")) { mongoTemplate.createCollection("auditoria"); }
        mongoTemplate.indexOps(com.umc.pediupatrao.entity.Usuario.class).ensureIndex(new org.springframework.data.mongodb.core.index.Index().on("username", org.springframework.data.domain.Sort.Direction.ASC).unique());
        if (!mongoTemplate.collectionExists("pedidos")) {
            mongoTemplate.createCollection("pedidos");
        }
        mongoTemplate.indexOps("pedidos").ensureIndex(new org.springframework.data.mongodb.core.index.Index()
                .on("dataEntrada", org.springframework.data.domain.Sort.Direction.ASC)
                .on("status", org.springframework.data.domain.Sort.Direction.ASC).named("dashboard_periodo_status"));
        if (!mongoTemplate.collectionExists("clientes")) {
            mongoTemplate.createCollection("clientes");
        }
        if (!mongoTemplate.collectionExists("usuarios")) {
            mongoTemplate.createCollection("usuarios");
        }
        if (!mongoTemplate.collectionExists("produtos")) {
            mongoTemplate.createCollection("produtos");
        }
        for (String colecao : java.util.List.of("pedidos", "clientes")) {
            mongoTemplate.updateMulti(new org.springframework.data.mongodb.core.query.Query(
                org.springframework.data.mongodb.core.query.Criteria.where("versao").is(null)),
                new org.springframework.data.mongodb.core.query.Update().set("versao", 0L), colecao);
        }
        mongoTemplate.updateMulti(new org.springframework.data.mongodb.core.query.Query(
            new org.springframework.data.mongodb.core.query.Criteria().orOperator(
                org.springframework.data.mongodb.core.query.Criteria.where("role").is(null),
                org.springframework.data.mongodb.core.query.Criteria.where("role").is("USER"))),
            new org.springframework.data.mongodb.core.query.Update().set("role", "ATENDENTE"), "usuarios");
        if (!mongoTemplate.collectionExists("horariosAtendimento")) {
            mongoTemplate.createCollection("horariosAtendimento");
        }
    }
}
