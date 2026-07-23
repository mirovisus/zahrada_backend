package upce.fei.garden.exception;

/**
 * Vyhazuje se, když je uživatel přihlášen, ale nemá oprávnění k dané akci.
 * Zpracovává se v {@link GlobalExceptionHandler} a mapuje na HTTP 403.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
