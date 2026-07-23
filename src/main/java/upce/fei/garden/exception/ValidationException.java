package upce.fei.garden.exception;

import java.util.Collections;
import java.util.Map;

/**
 * Vyhazuje se pro vlastní (business) validační chyby mimo Bean Validation.
 * Zpracovává se v {@link GlobalExceptionHandler} a mapuje na HTTP 400.
 * Volitelně nese mapu chyb jednotlivých polí (název pole -> zpráva).
 */
public class ValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public ValidationException(String message) {
        super(message);
        this.fieldErrors = Collections.emptyMap();
    }

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors == null ? Collections.emptyMap() : fieldErrors;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
