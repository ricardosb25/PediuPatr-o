package com.umc.pediupatrao.config;
import com.umc.pediupatrao.entity.StatusPedido;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
@ReadingConverter
public class StatusPedidoReadConverter implements Converter<String, StatusPedido> {
    @Override
    public StatusPedido convert(String origem) {
        return switch (origem) {
            case "EM_PREPARO", "EM PREPARAÇÃO" -> StatusPedido.EM_PREPARACAO;
            case "ENTREGUE" -> StatusPedido.FINALIZADO;
            case "SAIU PARA ENTREGA" -> StatusPedido.SAIU_PARA_ENTREGA;
            default -> StatusPedido.valueOf(origem);
        };
    }
}