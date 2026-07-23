package upce.fei.garden.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS konfigurace jako {@link CorsConfigurationSource} bean namísto {@code WebMvcConfigurer.addCorsMappings}.
 * Spring Security čte CORS pravidla přímo z tohoto beanu (viz {@code SecurityConfig#securityFilterChain}) -
 * díky tomu se preflight OPTIONS požadavek vyřeší dřív, než k němu dorazí autorizace.
 */
@Configuration
public class CorsConfig {

    private static final String FRONTEND_ORIGIN = "http://localhost:5173";

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(FRONTEND_ORIGIN));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
