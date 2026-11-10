package com.umc.pediupatrao;
import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.controller.*;
import com.umc.pediupatrao.entity.*;
import com.umc.pediupatrao.service.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
@WebMvcTest({HomeController.class, AuditoriaController.class}) @Import(SecurityConfig.class)
class TelasTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private PedidoService pedidos;
    @MockitoBean private ClienteService clientes;
    @MockitoBean private ProdutoService produtos;
    @MockitoBean private UsuarioService usuarios;
    @MockitoBean private UsuarioDetailsService autenticacao;
    @MockitoBean private AuditoriaService auditoria;
    @BeforeEach void preparar() {
        for (String perfil : new String[]{"ADMIN","GERENTE","ATENDENTE"})
            when(autenticacao.loadUserByUsername(perfil)).thenReturn(User.withUsername(perfil).password("teste").roles(perfil).build());
        Pedido pedido = new Pedido(); pedido.setId("pedido"); pedido.setStatus(StatusPedido.PRONTO);
        pedido.setItens(List.of(new ItemPedido("produto", "Pizza", 2, new java.math.BigDecimal("25.00"))));
        when(pedidos.listarPedidos()).thenReturn(List.of(pedido));
        when(auditoria.consultar(0)).thenReturn(new PageImpl<>(List.of()));
    }
    @Test void renderizaPedidosParaTodosOsPerfis() throws Exception {
        for (String perfil : new String[]{"ADMIN","GERENTE","ATENDENTE"})
            mvc.perform(get("/pedidos").with(user(perfil).roles(perfil))).andExpect(status().isOk()).andExpect(content().string(containsString("Pizza")));
    }
    @Test void renderizaProdutosClientesAuditoriaECadastro() throws Exception {
        mvc.perform(get("/produtos").with(user("ADMIN").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/clientes").with(user("GERENTE").roles("GERENTE"))).andExpect(status().isOk());
        mvc.perform(get("/auditoria").with(user("ADMIN").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/usuarios/novo").with(user("ADMIN").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/clientes/novo").with(user("ATENDENTE").roles("ATENDENTE"))).andExpect(status().isOk());
    }
}