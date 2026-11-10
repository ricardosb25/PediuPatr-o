package com.umc.pediupatrao.config;

import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {

    private final UsuarioDetailsService usuarioDetailsService;

    public SecurityConfig(UsuarioDetailsService usuarioDetailsService) {
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(usuarioDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .addFilterBefore(new PerfilAtualFilter(usuarioDetailsService), org.springframework.security.web.access.intercept.AuthorizationFilter.class)
                .csrf(org.springframework.security.config.Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/dist/**", "/plugins/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/usuarios/**", "/api/usuarios/**", "/admin", "/configuracoes").hasRole("ADMIN")
                        .requestMatchers("/dashboard", "/api/dashboard/**").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers("/auditoria", "/api/auditoria/**").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/pedidos/**", "/pedidos", "/api/produtos/**", "/produtos").hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")
                        .requestMatchers("/api/pedidos/*/status", "/api/pedidos/*/desconto", "/api/pedidos/*/cancelamento").hasRole("GERENTE")
                        .requestMatchers("/api/pedidos/**").hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers("/clientes/novo", "/clientes/editar/**", "/clientes/salvar").hasRole("ATENDENTE")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/clientes/**", "/clientes").hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers("/api/clientes/**").hasRole("ATENDENTE")
                        .requestMatchers("/api/produtos/**").hasRole("ADMIN")
                        .requestMatchers("/", "/error").authenticated()
                        .anyRequest().denyAll()
                )
                .exceptionHandling(erros -> erros.defaultAuthenticationEntryPointFor(
                    new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED),
                    new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/api/**")))
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        .successHandler(authenticationSuccessHandler())
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            response.sendRedirect("/"); // Redireciona para a página inicial após login bem-sucedido
        };
    }
}
