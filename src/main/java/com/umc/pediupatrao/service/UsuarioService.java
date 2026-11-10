package com.umc.pediupatrao.service;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final OperadorService operador;
    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, OperadorService operador) {
        this.usuarioRepository = usuarioRepository; this.passwordEncoder = passwordEncoder; this.operador = operador;
    }
    public Optional<Usuario> buscarPorId(String identificador) { return usuarioRepository.findById(identificador); }
    public Optional<Usuario> buscarPorUsername(String username) { return usuarioRepository.findByUsername(username); }
    public Iterable<Usuario> listarTodos() { return usuarioRepository.findAll(); }
    private void validar(Usuario usuario, String identificador) {
        if (usuario.getUsername() == null || !usuario.getUsername().matches("[A-Za-z0-9._-]{3,60}"))
            throw new IllegalArgumentException("Usuário deve ter 3 a 60 letras, números, pontos, traços ou sublinhados");
        if (!Set.of("ADMIN", "GERENTE", "ATENDENTE").contains(usuario.getRole() == null ? "" : usuario.getRole()))
            throw new IllegalArgumentException("Perfil inválido");
        usuarioRepository.findByUsername(usuario.getUsername()).ifPresent(existente -> {
            if (!Objects.equals(existente.getId(), identificador)) throw new IllegalArgumentException("Nome de usuário já cadastrado");
        });
    }
    private String codificarSenha(String senha) {
        if (senha == null || senha.length() < 12 || senha.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Senha deve ter ao menos 12 caracteres e no máximo 72 bytes");
        return passwordEncoder.encode(senha);
    }
    public void salvarUsuario(Usuario usuario) {
        validar(usuario, null);
        usuario.setId(null); usuario.setPassword(codificarSenha(usuario.getPassword()));
        usuarioRepository.save(usuario);
    }
    public void atualizarUsuario(String identificador, Usuario atualizado) {
        Usuario existente = usuarioRepository.findById(identificador).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        validar(atualizado, identificador);
        if (existente.getUsername().equals(operador.usuario()) &&
            (!existente.getRole().equals(atualizado.getRole()) || !existente.getUsername().equals(atualizado.getUsername())))
            throw new IllegalArgumentException("Não é permitido alterar o próprio login ou perfil");
        existente.setUsername(atualizado.getUsername()); existente.setRole(atualizado.getRole());
        if (atualizado.getPassword() != null && !atualizado.getPassword().isBlank()) existente.setPassword(codificarSenha(atualizado.getPassword()));
        usuarioRepository.save(existente);
    }
    public void deletarUsuario(String identificador) {
        Usuario existente = usuarioRepository.findById(identificador).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (existente.getUsername().equals(operador.usuario())) throw new IllegalArgumentException("Não é permitido excluir o próprio usuário");
        usuarioRepository.deleteById(identificador);
    }
}
