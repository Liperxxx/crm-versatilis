package com.versatilis.crm.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Value("${app.cors.allowed-origins:http://localhost:3000,https://crm-versatilis.vercel.app}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"erro\": \"Não autorizado\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"erro\": \"Acesso negado\"}");
                })
            )
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Rotas públicas
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/health").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/management/actuator/health").permitAll()
                .requestMatchers("/management/actuator/info").permitAll()
                .requestMatchers("/management/actuator/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()

                // Rotas administrativas - apenas ADMIN
                .requestMatchers(HttpMethod.POST, "/usuarios").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/usuarios/**").hasRole("ADMIN")
                .requestMatchers("/usuarios/me/**").authenticated()
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Configurações da empresa
                .requestMatchers(HttpMethod.POST, "/config/logo").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/config/**").authenticated()

                // Rotas de gerenciamento - ADMIN e GERENTE
                .requestMatchers(HttpMethod.POST, "/clientes").hasAnyRole("ADMIN", "GERENTE")
                .requestMatchers(HttpMethod.PUT, "/clientes/**").hasAnyRole("ADMIN", "GERENTE")
                .requestMatchers(HttpMethod.DELETE, "/clientes/**").hasAnyRole("ADMIN", "GERENTE")

                .requestMatchers(HttpMethod.POST, "/leads").hasAnyRole("ADMIN", "GERENTE", "OPERADOR")
                .requestMatchers(HttpMethod.PUT, "/leads/**").hasAnyRole("ADMIN", "GERENTE", "OPERADOR")
                .requestMatchers(HttpMethod.PATCH, "/leads/**").hasAnyRole("ADMIN", "GERENTE", "OPERADOR")
                .requestMatchers(HttpMethod.DELETE, "/leads/**").hasAnyRole("ADMIN", "GERENTE")

                .requestMatchers(HttpMethod.POST, "/produtos").hasAnyRole("ADMIN", "GERENTE")
                .requestMatchers(HttpMethod.PUT, "/produtos/**").hasAnyRole("ADMIN", "GERENTE")
                .requestMatchers(HttpMethod.PATCH, "/produtos/**").hasAnyRole("ADMIN", "GERENTE")
                .requestMatchers(HttpMethod.DELETE, "/produtos/**").hasAnyRole("ADMIN", "GERENTE")

                .requestMatchers(HttpMethod.POST, "/oportunidades").hasAnyRole("ADMIN", "GERENTE", "OPERADOR")
                .requestMatchers(HttpMethod.PUT, "/oportunidades/**").hasAnyRole("ADMIN", "GERENTE", "OPERADOR")
                .requestMatchers(HttpMethod.DELETE, "/oportunidades/**").hasAnyRole("ADMIN", "GERENTE")

                .requestMatchers(HttpMethod.POST, "/tarefas").hasAnyRole("ADMIN", "GERENTE", "OPERADOR")
                .requestMatchers(HttpMethod.PUT, "/tarefas/**").hasAnyRole("ADMIN", "GERENTE", "OPERADOR")
                .requestMatchers(HttpMethod.PATCH, "/tarefas/**").hasAnyRole("ADMIN", "GERENTE", "OPERADOR")
                .requestMatchers(HttpMethod.DELETE, "/tarefas/**").hasAnyRole("ADMIN", "GERENTE")

                .requestMatchers(HttpMethod.POST, "/orcamentos").hasAnyRole("ADMIN", "GERENTE", "OPERADOR")
                .requestMatchers(HttpMethod.PUT, "/orcamentos/**").hasAnyRole("ADMIN", "GERENTE", "OPERADOR")
                .requestMatchers(HttpMethod.DELETE, "/orcamentos/**").hasAnyRole("ADMIN", "GERENTE", "OPERADOR")

                .requestMatchers(HttpMethod.GET, "/relatorios/**").hasAnyRole("ADMIN", "GERENTE", "OPERADOR")

                .requestMatchers("/conversoes/**").hasAnyRole("ADMIN", "GERENTE", "OPERADOR")

                // Rotas de leitura - todos autenticados
                .requestMatchers(HttpMethod.GET, "/**").authenticated()

                // Qualquer outra requisição requer autenticação
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}