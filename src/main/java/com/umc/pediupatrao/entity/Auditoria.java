package com.umc.pediupatrao.entity;
import java.time.Instant;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
@Document(collection = "auditoria")
public record Auditoria(@Id String id, String usuario, String perfil, Instant dataHora,
    String operacao, String entidade, String entidadeId, Map<String, Alteracao> campos, String justificativa) {
    public record Alteracao(Object anterior, Object novo) {}
}
