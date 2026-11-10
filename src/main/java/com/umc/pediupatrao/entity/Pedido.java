package com.umc.pediupatrao.entity;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
@Getter @Setter
@Document(collection = "pedidos")
public class Pedido {
    @Id private String id;
    @Version private Long versao;
    private String clienteId;
    private String tipoPizza;
    private String sabor;
    private int quantidade;
    private StatusPedido status;
    private List<ItemPedido> itens;
    private BigDecimal subtotal;
    private BigDecimal descontoPercentual;
    private BigDecimal valorTotal;
    private String responsavelRegistro;
    private Instant dataEntrada;
    private String responsavelSaida;
    private Instant dataSaida;
    private String justificativaCancelamento;
}
