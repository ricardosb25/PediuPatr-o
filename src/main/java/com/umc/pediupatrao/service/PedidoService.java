package com.umc.pediupatrao.service;
import com.umc.pediupatrao.dto.NovoPedido;
import com.umc.pediupatrao.entity.*;
import com.umc.pediupatrao.repository.*;
import java.math.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class PedidoService {
    private final PedidoRepository pedidos;
    private final ProdutoRepository produtos;
    private final ClienteRepository clientes;
    private final AuditoriaService auditoria;
    private final OperadorService operador;
    public PedidoService(PedidoRepository pedidos, ProdutoRepository produtos, ClienteRepository clientes,
                         AuditoriaService auditoria, OperadorService operador) {
        this.pedidos = pedidos; this.produtos = produtos; this.clientes = clientes;
        this.auditoria = auditoria; this.operador = operador;
    }
    @Transactional
    @PreAuthorize("hasAnyRole('ATENDENTE','GERENTE')")
    public Pedido criarPedido(NovoPedido solicitacao) {
        if (solicitacao.clienteId() == null || !clientes.existsById(solicitacao.clienteId()))
            throw new IllegalArgumentException("Cliente inválido");
        if (solicitacao.itens() == null || solicitacao.itens().isEmpty() || solicitacao.itens().size() > 100)
            throw new IllegalArgumentException("Informe de 1 a 100 itens");
        List<ItemPedido> itens = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (NovoPedido.ItemSolicitado solicitado : solicitacao.itens()) {
            if (solicitado == null || solicitado.produtoId() == null || solicitado.quantidade() < 1 || solicitado.quantidade() > 1000)
                throw new IllegalArgumentException("Produto e quantidade inválidos");
            Produto produto = produtos.findById(solicitado.produtoId()).orElseThrow(() -> new IllegalArgumentException("Produto inexistente"));
            if (!Boolean.TRUE.equals(produto.getAtivo()) || produto.getPreco() == null || !Double.isFinite(produto.getPreco()) || produto.getPreco() < 0)
                throw new IllegalArgumentException("Produto indisponível ou preço inválido");
            BigDecimal preco = BigDecimal.valueOf(produto.getPreco()).setScale(2, RoundingMode.HALF_UP);
            itens.add(new ItemPedido(produto.getId(), produto.getNome(), solicitado.quantidade(), preco));
            subtotal = subtotal.add(preco.multiply(BigDecimal.valueOf(solicitado.quantidade())));
        }
        Pedido pedido = new Pedido();
        pedido.setClienteId(solicitacao.clienteId()); pedido.setItens(itens);
        pedido.setSubtotal(subtotal); pedido.setDescontoPercentual(BigDecimal.ZERO); pedido.setValorTotal(subtotal);
        pedido.setStatus(StatusPedido.RECEBIDO); pedido.setDataEntrada(Instant.now()); pedido.setResponsavelRegistro(operador.usuario());
        Pedido salvo = pedidos.save(pedido);
        auditoria.registrar("INCLUSAO_PEDIDO", "Pedido", salvo.getId(), Map.of(), salvo, null);
        return salvo;
    }
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public List<Pedido> listarPedidos() { return pedidos.findAll(); }
    private Pedido buscar(String identificador) {
        return pedidos.findById(identificador).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));
    }
    @Transactional
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido atualizarStatus(String identificador, String status) {
        Pedido pedido = buscar(identificador);
        StatusPedido destino;
        try { destino = StatusPedido.valueOf(status); }
        catch (RuntimeException falha) { throw new IllegalArgumentException("Status inválido"); }
        if (pedido.getStatus() == null || !pedido.getStatus().permiteTransicao(destino))
            throw new IllegalArgumentException("Transição de status não permitida");
        var anterior = auditoria.retrato(pedido);
        pedido.setStatus(destino);
        if (destino == StatusPedido.SAIU_PARA_ENTREGA || destino == StatusPedido.RETIRADO) {
            pedido.setDataSaida(Instant.now()); pedido.setResponsavelSaida(operador.usuario());
        }
        Pedido salvo = pedidos.save(pedido);
        auditoria.registrar("ALTERACAO_STATUS", "Pedido", salvo.getId(), anterior, salvo, null);
        return salvo;
    }
    private void validarAjuste(Pedido pedido) {
        if (pedido.getStatus() == null || !pedido.getStatus().permiteAjustes() || pedido.getDataSaida() != null)
            throw new IllegalArgumentException("Pedido não permite desconto ou cancelamento neste estado");
    }
    @Transactional
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String identificador, BigDecimal percentual) {
        Pedido pedido = buscar(identificador); validarAjuste(pedido);
        if (percentual == null || percentual.signum() < 0 || percentual.compareTo(new BigDecimal("20")) > 0)
            throw new IllegalArgumentException("Desconto deve estar entre 0 e 20%");
        if (pedido.getSubtotal() == null) throw new IllegalArgumentException("Pedido legado sem valores: não pode receber desconto");
        var anterior = auditoria.retrato(pedido);
        pedido.setDescontoPercentual(percentual);
        pedido.setValorTotal(pedido.getSubtotal().multiply(BigDecimal.ONE.subtract(percentual.movePointLeft(2))).setScale(2, RoundingMode.HALF_UP));
        Pedido salvo = pedidos.save(pedido);
        auditoria.registrar("DESCONTO_PEDIDO", "Pedido", salvo.getId(), anterior, salvo, null);
        return salvo;
    }
    @Transactional
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelarPedido(String identificador, String justificativa) {
        Pedido pedido = buscar(identificador); validarAjuste(pedido);
        if (justificativa == null || justificativa.isBlank() || justificativa.length() > 1000)
            throw new IllegalArgumentException("Informe justificativa de até 1000 caracteres");
        var anterior = auditoria.retrato(pedido);
        pedido.setStatus(StatusPedido.CANCELADO); pedido.setJustificativaCancelamento(justificativa.trim());
        Pedido salvo = pedidos.save(pedido);
        auditoria.registrar("CANCELAMENTO_PEDIDO", "Pedido", salvo.getId(), anterior, salvo, justificativa.trim());
        return salvo;
    }
}
