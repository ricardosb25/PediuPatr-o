package com.umc.pediupatrao.config;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;
public class PerfilAtualFilter extends OncePerRequestFilter {
    private final UsuarioDetailsService usuarios;
    public PerfilAtualFilter(UsuarioDetailsService usuarios) { this.usuarios = usuarios; }
    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia) throws ServletException, IOException {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.isAuthenticated() && !"anonymousUser".equals(autenticacao.getName())) {
            try {
                var usuario = usuarios.loadUserByUsername(autenticacao.getName());
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
            } catch (UsernameNotFoundException falha) {
                SecurityContextHolder.clearContext();
                if (requisicao.getSession(false) != null) requisicao.getSession(false).invalidate();
                resposta.sendError(401); return;
            }
        }
        cadeia.doFilter(requisicao, resposta);
    }
}