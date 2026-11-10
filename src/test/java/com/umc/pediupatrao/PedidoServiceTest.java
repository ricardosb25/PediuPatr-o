package com.umc.pediupatrao;
import com.umc.pediupatrao.dto.NovoPedido;
import com.umc.pediupatrao.entity.*;
import com.umc.pediupatrao.repository.*;
import com.umc.pediupatrao.service.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PedidoServiceTest {
    private PedidoRepository pedidos;
    private ProdutoRepository produtos;
    private ClienteRepository clientes;
    private AuditoriaService auditoria;
    private PedidoService service;
    private Pedido pedido;
    @BeforeEach void preparar() {
        pedidos = mock(PedidoRepository.class); produtos = mock(ProdutoRepository.class); clientes = mock(ClienteRepository.class);
        auditoria = mock(AuditoriaService.class); OperadorService operador = mock(OperadorService.class);
        when(operador.usuario()).thenReturn("gerente"); when(auditoria.retrato(any())).thenReturn(Map.of());
        when(pedidos.save(any())).thenAnswer(invocacao -> { Pedido salvo = invocacao.getArgument(0); salvo.setId("pedido"); return salvo; });
        service = new PedidoService(pedidos, produtos, clientes, auditoria, operador);
        pedido = new Pedido(); pedido.setId("pedido"); pedido.setStatus(StatusPedido.RECEBIDO); pedido.setSubtotal(new BigDecimal("100.00"));
        when(pedidos.findById("pedido")).thenReturn(Optional.of(pedido));
    }
    @Test void calculaPrecoDoCatalogoERegistraEntrada() {
        Produto produto = new Produto(); produto.setId("pizza"); produto.setNome("Pizza"); produto.setPreco(29.90); produto.setAtivo(true);
        when(produtos.findById("pizza")).thenReturn(Optional.of(produto)); when(clientes.existsById("cliente")).thenReturn(true);
        Pedido salvo = service.criarPedido(new NovoPedido("cliente", List.of(new NovoPedido.ItemSolicitado("pizza", 2))));
        assertEquals(new BigDecimal("59.80"), salvo.getValorTotal()); assertEquals(StatusPedido.RECEBIDO, salvo.getStatus());
        assertNotNull(salvo.getDataEntrada()); assertEquals("gerente", salvo.getResponsavelRegistro());
        verify(auditoria).registrar(eq("INCLUSAO_PEDIDO"), eq("Pedido"), eq("pedido"), anyMap(), same(salvo), isNull());
    }
    @Test void rejeitaQuantidadesInvalidas() {
        when(clientes.existsById("cliente")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.criarPedido(new NovoPedido("cliente", List.of(new NovoPedido.ItemSolicitado("pizza", 0)))));
        verify(pedidos, never()).save(any());
    }
    @Test void exigeFluxoERegistraSaida() {
        assertThrows(IllegalArgumentException.class, () -> service.atualizarStatus("pedido", "FINALIZADO"));
        service.atualizarStatus("pedido", "EM_PREPARACAO"); service.atualizarStatus("pedido", "PRONTO");
        service.atualizarStatus("pedido", "RETIRADO"); assertNotNull(pedido.getDataSaida()); assertEquals("gerente", pedido.getResponsavelSaida());
        assertThrows(IllegalArgumentException.class, () -> service.cancelarPedido("pedido", "Motivo"));
        assertThrows(IllegalArgumentException.class, () -> service.aplicarDesconto("pedido", BigDecimal.TEN));
        service.atualizarStatus("pedido", "FINALIZADO");
        assertThrows(IllegalArgumentException.class, () -> service.atualizarStatus("pedido", "RECEBIDO"));
    }
    @Test void descontoLimitadoESemAcumulo() {
        assertThrows(IllegalArgumentException.class, () -> service.aplicarDesconto("pedido", new BigDecimal("20.01")));
        assertThrows(IllegalArgumentException.class, () -> service.aplicarDesconto("pedido", new BigDecimal("-1")));
        service.aplicarDesconto("pedido", new BigDecimal("20")); assertEquals(new BigDecimal("80.00"), pedido.getValorTotal());
        service.aplicarDesconto("pedido", BigDecimal.TEN); assertEquals(new BigDecimal("90.00"), pedido.getValorTotal());
    }
    @Test void cancelamentoExigeJustificativaEBloqueiaAlteracoes() {
        assertThrows(IllegalArgumentException.class, () -> service.cancelarPedido("pedido", " "));
        service.cancelarPedido("pedido", "Cliente desistiu"); assertEquals(StatusPedido.CANCELADO, pedido.getStatus());
        assertThrows(IllegalArgumentException.class, () -> service.atualizarStatus("pedido", "EM_PREPARACAO"));
        assertThrows(IllegalArgumentException.class, () -> service.aplicarDesconto("pedido", BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> service.cancelarPedido("pedido", "Outra justificativa"));
    }
    @Test void validaTodasAsTransicoes() {
        for (StatusPedido origem : StatusPedido.values()) {
            for (StatusPedido destino : StatusPedido.values()) {
                boolean esperado = (origem == StatusPedido.RECEBIDO && destino == StatusPedido.EM_PREPARACAO)
                    || (origem == StatusPedido.EM_PREPARACAO && destino == StatusPedido.PRONTO)
                    || (origem == StatusPedido.PRONTO && (destino == StatusPedido.RETIRADO || destino == StatusPedido.SAIU_PARA_ENTREGA))
                    || ((origem == StatusPedido.RETIRADO || origem == StatusPedido.SAIU_PARA_ENTREGA) && destino == StatusPedido.FINALIZADO);
                assertEquals(esperado, origem.permiteTransicao(destino), origem + " -> " + destino);
            }
        }
    }
}