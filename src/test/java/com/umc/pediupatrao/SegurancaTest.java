package com.umc.pediupatrao;
import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.controller.PedidoController;
import com.umc.pediupatrao.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(PedidoController.class)
@Import(SecurityConfig.class)
class SegurancaTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private PedidoService pedidos;
    @MockitoBean private UsuarioDetailsService usuarios;
    @BeforeEach void preparar() {
        for (String perfil : new String[]{"ADMIN","GERENTE","ATENDENTE"})
            when(usuarios.loadUserByUsername(perfil)).thenReturn(User.withUsername(perfil).password("senha-teste").roles(perfil).build());
    }
    @Test void somenteGerenteAplicaDescontoInclusiveContraAdmin() throws Exception {
        for (String perfil : new String[]{"ADMIN","ATENDENTE"}) {
            mvc.perform(put("/api/pedidos/pedido/desconto").with(user(perfil).roles(perfil)).with(csrf())
                .contentType("application/json").content("{\"percentual\":10}")).andExpect(status().isForbidden());
        }
        mvc.perform(put("/api/pedidos/pedido/desconto").with(user("GERENTE").roles("GERENTE")).with(csrf())
            .contentType("application/json").content("{\"percentual\":10}")).andExpect(status().isOk());
    }
    @Test void bloqueiaCancelamentoEStatusDeAtendente() throws Exception {
        mvc.perform(put("/api/pedidos/pedido/cancelamento").with(user("ATENDENTE").roles("ATENDENTE")).with(csrf())
            .contentType("application/json").content("{\"justificativa\":\"Motivo\"}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/pedidos/pedido/status").param("status", "PRONTO").with(user("ATENDENTE").roles("ATENDENTE")).with(csrf())).andExpect(status().isForbidden());
        verifyNoInteractions(pedidos);
    }
    @Test void exigeCsrf() throws Exception {
        mvc.perform(put("/api/pedidos/pedido/desconto").with(user("GERENTE").roles("GERENTE"))
            .contentType("application/json").content("{\"percentual\":10}")).andExpect(status().isForbidden());
        verifyNoInteractions(pedidos);
    }
    @Test void protegeCadastroEAuditoria() throws Exception {
        mvc.perform(post("/api/usuarios").with(csrf()).contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auditoria").with(user("ATENDENTE").roles("ATENDENTE"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/usuarios").with(user("GERENTE").roles("GERENTE")).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isForbidden());
    }
}