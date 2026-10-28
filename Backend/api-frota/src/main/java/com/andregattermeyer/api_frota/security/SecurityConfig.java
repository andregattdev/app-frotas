package com.andregattermeyer.api_frota.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.andregattermeyer.api_frota.config.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    // Construtor explícito
    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, UserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/registrar").permitAll()
                // LIBERA OS CAMINHOS DO SWAGGER UI E OPENAPI (SPRINGDOC)
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/auth/alterar-senha").hasAnyRole("MOTORISTA", "ASSISTENTE_FROTA")
                .requestMatchers(HttpMethod.GET, "/api/rotas/minhas").hasRole("MOTORISTA")
                .requestMatchers(HttpMethod.GET, "/api/rotas/minhas/encaminhamentos").hasRole("MOTORISTA")
                .requestMatchers("/api/rotas/**").hasRole("MOTORISTA")
                .requestMatchers(HttpMethod.GET, "/api/checklists/veiculo/*").hasAnyRole("MOTORISTA", "ADMIN", "SUPER_ADMIN", "ASSISTENTE_FROTA")
                .requestMatchers(HttpMethod.POST, "/api/checklists").hasAnyRole("MOTORISTA", "ADMIN", "SUPER_ADMIN", "ASSISTENTE_FROTA")
                .requestMatchers(HttpMethod.GET, "/api/veiculos", "/api/veiculos/*").hasAnyRole("ASSISTENTE_FROTA", "ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/veiculos").hasAnyRole("ASSISTENTE_FROTA", "ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/veiculos/*").hasAnyRole("ASSISTENTE_FROTA", "ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/veiculos/*").hasAnyRole("ASSISTENTE_FROTA", "ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/oficinas/**").hasAnyRole("ASSISTENTE_FROTA", "ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/enderecos/consulta-cep/*").hasAnyRole("ASSISTENTE_FROTA", "ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/assistente/**").hasAnyRole("ASSISTENTE_FROTA", "ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);  

        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
