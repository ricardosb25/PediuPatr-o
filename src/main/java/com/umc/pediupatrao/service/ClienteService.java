package com.umc.pediupatrao.service;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@Service
public class ClienteService {
    private final ClienteRepository clientes;
    private final PedidoRepository pedidos;
    private final AuditoriaService auditoria;
    public ClienteService(ClienteRepository clientes, PedidoRepository pedidos, AuditoriaService auditoria) {
        this.clientes = clientes; this.pedidos = pedidos; this.auditoria = auditoria;
    }
    @Transactional @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente novoCliente(Cliente cliente) { cliente.setId(null); return salvar(cliente); }
    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public List<Cliente> listarClientes() { return clientes.findAll(); }
    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public Optional<Cliente> buscarPorId(String identificador) { return clientes.findById(identificador); }
    @Transactional @PreAuthorize("hasRole('ATENDENTE')")
    public void excluir(String identificador) {
        Cliente cliente = clientes.findById(identificador).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!pedidos.findByClienteId(identificador).isEmpty()) throw new IllegalArgumentException("Cliente possui histórico de pedidos e não pode ser excluído");
        auditoria.registrar("EXCLUSAO_CLIENTE", "Cliente", identificador, auditoria.retrato(cliente), null, null);
        clientes.deleteById(identificador);
    }
    @Transactional @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente salvar(Cliente cliente) {
        if (cliente.getNome() == null || cliente.getNome().isBlank() || cliente.getTelefone() == null || cliente.getTelefone().isBlank())
            throw new IllegalArgumentException("Nome e telefone são obrigatórios");
        Cliente anterior = cliente.getId() == null ? null : clientes.findById(cliente.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (anterior != null) cliente.setVersao(anterior.getVersao());
        var retrato = auditoria.retrato(anterior);
        Cliente salvo = clientes.save(cliente);
        auditoria.registrar(anterior == null ? "INCLUSAO_CLIENTE" : "ALTERACAO_CLIENTE", "Cliente", salvo.getId(), retrato, salvo, null);
        return salvo;
    }
}
