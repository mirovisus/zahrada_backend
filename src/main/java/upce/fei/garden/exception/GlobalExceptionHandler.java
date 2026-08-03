package upce.fei.garden.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centrální zpracování výjimek – převádí je na jednotný formát {@link ApiError}.
 * <p>
 * Očekávané chyby (4xx – špatný vstup, konflikt, chybějící záznam, chybějící oprávnění) se logují
 * na úrovni WARN jen se zprávou, bez stack trace – jde o běžné, předvídatelné stavy aplikace.
 * Neočekávaná chyba ({@link #handleGenericException}, HTTP 500) se loguje na úrovni ERROR i s celým
 * stack trace, protože jde o skutečnou závadu, kterou je potřeba dohledat v kódu.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        logExpected(HttpStatus.NOT_FOUND, request, ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        logExpected(HttpStatus.FORBIDDEN, request, ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        logExpected(HttpStatus.FORBIDDEN, request, ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "Nemáte oprávnění k této akci.", request, null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest request) {
        logExpected(HttpStatus.CONFLICT, request, ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        logExpected(HttpStatus.UNAUTHORIZED, request, ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "Neplatný e-mail nebo heslo.", request, null);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidation(ValidationException ex, HttpServletRequest request) {
        logExpected(HttpStatus.BAD_REQUEST, request, ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, ex.getFieldErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        logExpected(HttpStatus.BAD_REQUEST, request, "Validace vstupních dat selhala: " + fieldErrors);
        return buildResponse(HttpStatus.BAD_REQUEST, "Validace vstupních dat selhala.", request, fieldErrors);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiError> handleNoRouteFound(Exception ex, HttpServletRequest request) {
        logExpected(HttpStatus.NOT_FOUND, request, ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "Požadovaná cesta nebyla nalezena.", request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                               HttpServletRequest request) {
        String message = "HTTP metoda '" + ex.getMethod() + "' není pro tuto cestu podporována.";
        logExpected(HttpStatus.METHOD_NOT_ALLOWED, request, message);
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, message, request, null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex,
                                                                  HttpServletRequest request) {
        // multipart parser odmitne prilis velky soubor drive, nez se dostane k FileStorageService#store
        String message = "Nahrávaný soubor je příliš velký.";
        logExpected(HttpStatus.BAD_REQUEST, request, message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMessageNotReadable(HttpMessageNotReadableException ex,
                                                               HttpServletRequest request) {
        logExpected(HttpStatus.BAD_REQUEST, request, ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Tělo požadavku obsahuje neplatný nebo poškozený JSON.", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex, HttpServletRequest request) {
        // Neočekávaná chyba - na rozdíl od ostatních handlerů logujeme na ERROR i s celým stack trace,
        // protože jde o skutečnou závadu (bug, výpadek závislosti apod.), ne běžný stav aplikace.
        log.error("Neočekávaná chyba při zpracování požadavku: {} {}",
                request.getMethod(), request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Nastala neočekávaná chyba.", request, null);
    }

    private void logExpected(HttpStatus status, HttpServletRequest request, String message) {
        log.warn("Očekávaná chyba {} {}: {} {} – {}",
                status.value(), status.getReasonPhrase(), request.getMethod(), request.getRequestURI(), message);
    }

    private ResponseEntity<ApiError> buildResponse(HttpStatus status, String message, HttpServletRequest request,
                                                     Map<String, String> fieldErrors) {
        ApiError apiError = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(status).body(apiError);
    }
}
