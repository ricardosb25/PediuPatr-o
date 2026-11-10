package com.umc.pediupatrao.config;
import org.springframework.context.annotation.*;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
@Configuration
public class MongoTransactionConfig {
    @Bean
    public org.springframework.data.mongodb.core.convert.MongoCustomConversions mongoCustomConversions() {
        return new org.springframework.data.mongodb.core.convert.MongoCustomConversions(java.util.List.of(new StatusPedidoReadConverter()));
    }
    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory factory) {
        return new MongoTransactionManager(factory);
    }
}
