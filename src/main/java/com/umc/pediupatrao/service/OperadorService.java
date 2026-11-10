package com.umc.pediupatrao.service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
@Service
public class OperadorService {
    public String usuario() {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated() || "anonymousUser".equals(autenticacao.getName())) {
            throw new AccessDeniedException("Autenticação obrigatória");
        }
        return autenticacao.getName();
    }
    public String perfil() {
        usuario();
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
            .map(autoridade -> autoridade.getAuthority()).filter(autoridade -> autoridade.startsWith("ROLE_"))
            .findFirst().orElseThrow(() -> new AccessDeniedException("Perfil inválido")).substring(5);
    }
}
