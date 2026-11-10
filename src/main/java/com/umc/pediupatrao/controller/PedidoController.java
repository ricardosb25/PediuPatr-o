package com.umc.pediupatrao.controller;
import com.umc.pediupatrao.dto.NovoPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.service.PedidoService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    private final PedidoService service;
    public PedidoController(PedidoService service) { this.service = service; }
    @PostMapping public Pedido criarPedido(@RequestBody NovoPedido pedido) { return service.criarPedido(pedido); }
    @GetMapping public List<Pedido> listarPedidos() { return service.listarPedidos(); }
    @PutMapping("/{id}/status") public Pedido atualizarStatus(@PathVariable String id, @RequestParam String status) {
        return service.atualizarStatus(id, status);
    }
    @PutMapping("/{id}/desconto") public Pedido aplicarDesconto(@PathVariable String id, @RequestBody Desconto solicitacao) {
        return service.aplicarDesconto(id, solicitacao.percentual());
    }
    @PutMapping("/{id}/cancelamento") public Pedido cancelarPedido(@PathVariable String id, @RequestBody Cancelamento solicitacao) {
        return service.cancelarPedido(id, solicitacao.justificativa());
    }
    public record Desconto(BigDecimal percentual) {}
    public record Cancelamento(String justificativa) {}
}
