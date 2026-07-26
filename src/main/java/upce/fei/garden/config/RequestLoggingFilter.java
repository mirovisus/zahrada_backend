package upce.fei.garden.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Loguje na úrovni INFO každý požadavek na {@code /api/**} – HTTP metodu, cestu, výsledný stavový
 * kód a dobu zpracování. Neloguje hlavičky ani tělo požadavku/odpovědi, takže se do logu nikdy
 * nedostane heslo ani JWT token (ten chodí pouze v hlavičce {@code Authorization}).
 * <p>
 * Filtr má nejvyšší prioritu ({@link Ordered#HIGHEST_PRECEDENCE}), aby obalil celé zpracování
 * požadavku včetně Spring Security řetězce – díky tomu se do logu dostane i výsledný stav
 * a doba trvání požadavků odmítnutých autentizací/autorizací (401/403).
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            log.info("{} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }
}
