package com.petsalud.backendpetsalud.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración central de Spring Security para CLÍNICA PETSALUD.
 *
 * Estrategia: API REST stateless con JWT.
 *  - Sin sesión en servidor (STATELESS) y sin cookies de autenticación.
 *  - CSRF deshabilitado de forma JUSTIFICADA: el token viaja en el header Authorization,
 *    que el navegador nunca envía automáticamente, por lo que un sitio malicioso no puede
 *    forjar peticiones autenticadas (CSRF depende de credenciales enviadas automáticamente).
 *  - Reglas por URL (primera barrera) + @PreAuthorize por método (segunda barrera, más fina).
 *  - Headers de seguridad contra XSS / clickjacking / MIME sniffing.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // habilita @PreAuthorize
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final UsuarioDetailsService usuarioDetailsService;
    private final RestSecurityHandlers securityHandlers;

    @Value("${security.cors.allowed-origins:http://localhost:3000}")
    private List<String> allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                          UsuarioDetailsService usuarioDetailsService,
                          RestSecurityHandlers securityHandlers) {
        this.jwtFilter = jwtFilter;
        this.usuarioDetailsService = usuarioDetailsService;
        this.securityHandlers = securityHandlers;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(securityHandlers)
                        .accessDeniedHandler(securityHandlers))
                .headers(h -> h
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
                        .frameOptions(f -> f.deny())
                        .contentTypeOptions(c -> { })
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)))
                .authorizeHttpRequests(auth -> auth
                        // Públicos
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/registro").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Solo GERENTE
                        .requestMatchers(HttpMethod.GET, "/api/usuarios").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.DELETE, "/api/usuarios/**").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.GET, "/api/ventas").hasRole("GERENTE")
                        // Personal interno (EMPLEADO o GERENTE)
                        .requestMatchers(HttpMethod.POST, "/api/productos").hasAnyRole("EMPLEADO", "GERENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/productos/**").hasAnyRole("EMPLEADO", "GERENTE")
                        .requestMatchers(HttpMethod.DELETE, "/api/productos/**").hasRole("GERENTE")
                        .requestMatchers("/api/productos/stock-bajo").hasAnyRole("EMPLEADO", "GERENTE")
                        .requestMatchers(HttpMethod.POST, "/api/expedientes").hasAnyRole("EMPLEADO", "GERENTE")
                        .requestMatchers("/api/citas/veterinario/**", "/api/citas/entre-fechas")
                            .hasAnyRole("EMPLEADO", "GERENTE")
                        // Todo lo demás: autenticado (y luego @PreAuthorize decide propiedad del dato)
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Evita que Spring Boot registre el filtro JWT dos veces (solo debe correr dentro de la cadena de seguridad). */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registro = new FilterRegistrationBean<>(filter);
        registro.setEnabled(false);
        return registro;
    }

    /** BCrypt con factor de costo 12 (hash lento y con sal: resiste fuerza bruta offline). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(authenticationProvider());
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
