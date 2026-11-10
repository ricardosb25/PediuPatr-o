package com.umc.pediupatrao.entity;
public enum StatusPedido {
    RECEBIDO, EM_PREPARACAO, PRONTO, SAIU_PARA_ENTREGA, RETIRADO, FINALIZADO, CANCELADO;
    public boolean permiteAjustes() {
        return this == RECEBIDO || this == EM_PREPARACAO || this == PRONTO;
    }
    public boolean permiteTransicao(StatusPedido destino) {
        return switch (this) {
            case RECEBIDO -> destino == EM_PREPARACAO;
            case EM_PREPARACAO -> destino == PRONTO;
            case PRONTO -> destino == SAIU_PARA_ENTREGA || destino == RETIRADO;
            case SAIU_PARA_ENTREGA, RETIRADO -> destino == FINALIZADO;
            default -> false;
        };
    }
}
