package com.umc.pediupatrao.dto;
import java.util.List;
public record NovoPedido(String clienteId, List<ItemSolicitado> itens) {
    public record ItemSolicitado(String produtoId, int quantidade) {}
}
