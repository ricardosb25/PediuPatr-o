package com.umc.pediupatrao;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.pediupatrao.entity.*;
import com.umc.pediupatrao.repository.*;
import com.umc.pediupatrao.service.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ClienteAuditoriaTest {
    @Test void registraTelefoneEEnderecoComAntesEDepois() {
        ClienteRepository clientes = mock(ClienteRepository.class); PedidoRepository pedidos = mock(PedidoRepository.class);
        AuditoriaRepository registros = mock(AuditoriaRepository.class); OperadorService operador = mock(OperadorService.class);
        when(operador.usuario()).thenReturn("atendente"); when(operador.perfil()).thenReturn("ATENDENTE");
        AuditoriaService auditoria = new AuditoriaService(registros, operador, new ObjectMapper().findAndRegisterModules());
        ClienteService service = new ClienteService(clientes, pedidos, auditoria);
        Cliente anterior = new Cliente(); anterior.setId("cliente"); anterior.setNome("Maria"); anterior.setTelefone("1111"); anterior.setLogradouro("Rua antiga");
        Cliente atualizado = new Cliente(); atualizado.setId("cliente"); atualizado.setNome("Maria"); atualizado.setTelefone("2222"); atualizado.setLogradouro("Rua nova");
        when(clientes.findById("cliente")).thenReturn(Optional.of(anterior)); when(clientes.save(atualizado)).thenReturn(atualizado);
        service.salvar(atualizado);
        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class); verify(registros).insert(captor.capture());
        Auditoria registro = captor.getValue(); assertEquals("ATENDENTE", registro.perfil()); assertEquals("atendente", registro.usuario());
        assertEquals("cliente", registro.entidadeId()); assertNotNull(registro.dataHora());
        assertEquals(new Auditoria.Alteracao("1111", "2222"), registro.campos().get("telefone"));
        assertEquals(new Auditoria.Alteracao("Rua antiga", "Rua nova"), registro.campos().get("logradouro"));
        assertFalse(registro.campos().containsKey("nome"));
    }
    @Test void naoIncluiCredenciaisNaAuditoria() {
        AuditoriaRepository registros = mock(AuditoriaRepository.class); OperadorService operador = mock(OperadorService.class);
        when(operador.usuario()).thenReturn("admin"); when(operador.perfil()).thenReturn("ADMIN");
        AuditoriaService auditoria = new AuditoriaService(registros, operador, new ObjectMapper());
        auditoria.registrar("TESTE", "Teste", "registro", Map.of("password", "anterior", "token", "anterior"),
            Map.of("password", "nova", "token", "novo", "nome", "Maria"), null);
        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class); verify(registros).insert(captor.capture());
        assertEquals(Set.of("nome"), captor.getValue().campos().keySet());
    }
    @Test void impedeExclusaoDeClienteComHistorico() {
        ClienteRepository clientes = mock(ClienteRepository.class); PedidoRepository pedidos = mock(PedidoRepository.class);
        when(clientes.findById("cliente")).thenReturn(Optional.of(new Cliente())); when(pedidos.findByClienteId("cliente")).thenReturn(List.of(new Pedido()));
        ClienteService service = new ClienteService(clientes, pedidos, mock(AuditoriaService.class));
        assertThrows(IllegalArgumentException.class, () -> service.excluir("cliente")); verify(clientes, never()).deleteById(anyString());
    }
}