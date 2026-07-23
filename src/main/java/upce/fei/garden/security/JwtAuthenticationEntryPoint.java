package upce.fei.garden.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import upce.fei.garden.exception.ApiError;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Bez tohoto handleru by Spring Security bez nakonfigurovaného httpBasic/formLogin
 * vracel na neautentizovaný požadavek výchozích 403 (Http403ForbiddenEntryPoint).
 * Pro stateless JWT API vracíme 401 v jednotném formátu {@link ApiError}, stejném jako {@code GlobalExceptionHandler}.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiError apiError = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message("Pro přístup k tomuto zdroji je vyžadována platná autentizace.")
                .path(request.getRequestURI())
                .build();

        objectMapper.writeValue(response.getWriter(), apiError);
    }
}
