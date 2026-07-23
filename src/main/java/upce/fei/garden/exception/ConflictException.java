package upce.fei.garden.exception;

/**
 * Vyhazuje se při konfliktu se stávajícím stavem dat (např. duplicitní e-mail).
 * Zpracovává se v {@link GlobalExceptionHandler} a mapuje na HTTP 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
