package upce.fei.garden.exception;

/**
 * Vyhazuje se, když požadovaný záznam (entita) v databázi neexistuje.
 * Zpracovává se v {@link GlobalExceptionHandler} a mapuje na HTTP 404.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
