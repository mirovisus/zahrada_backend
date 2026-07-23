package upce.fei.garden.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Konfigurace Spring Security pro stateless REST API se JWT autentizací.
 * <p>
 * Session se nepoužívá ({@link SessionCreationPolicy#STATELESS}), CSRF je vypnuté (nemá smysl bez session
 * a cookie), přístup k API vyžaduje platný JWT ověřovaný v {@link JwtAuthenticationFilter}, který je zařazen
 * před standardní {@link UsernamePasswordAuthenticationFilter}. Neautentizované požadavky na chráněné
 * zdroje řeší {@link JwtAuthenticationEntryPoint} (vrací 401 místo výchozích 403).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/api/auth/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/h2-console/**",
            "/actuator/health"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * BCrypt pro hashování hesel při registraci i pro ověření hesla při přihlášení.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * {@link AuthenticationManager} použitý v {@code AuthService} pro ověření přihlašovacích údajů.
     * Spring Security jej sestaví z jediného {@link org.springframework.security.core.userdetails.UserDetailsService}
     * ({@link CustomUserDetailsService}) a jediného {@link PasswordEncoder} bean v kontextu.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint))
                // H2 konzole vykresluje UI ve <frame>, výchozí DENY by ji zablokoval
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
