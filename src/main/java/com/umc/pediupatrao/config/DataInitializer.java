package com.umc.pediupatrao.config;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repository, PasswordEncoder encoder,
        @Value("${app.bootstrap.username:}") String username, @Value("${app.bootstrap.password:}") String password) {
        return argumentos -> {
            if (repository.findAll().stream().noneMatch(usuario -> "ADMIN".equals(usuario.getRole()))) {
                if (username.isBlank() || password.length() < 12 || password.length() > 72)
                    throw new IllegalStateException("Banco sem ADMIN: configure BOOTSTRAP_ADMIN_USERNAME e BOOTSTRAP_ADMIN_PASSWORD (12 a 72 caracteres)");
                if (repository.findByUsername(username.trim()).isPresent())
                    throw new IllegalStateException("BOOTSTRAP_ADMIN_USERNAME deve ser um login novo quando não há ADMIN");
                Usuario administrador = new Usuario();
                administrador.setUsername(username.trim()); administrador.setPassword(encoder.encode(password)); administrador.setRole("ADMIN");
                repository.save(administrador);
            }
        };
    }
}
