package upce.fei.garden.validation.rules;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import upce.fei.garden.validation.validator.CzechPhoneValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Vlastní validační pravidlo: ověřuje formát českého telefonního čísla – předvolba {@code +420}
 * a devět číslic, volitelně oddělených mezerami (např. {@code +420 123 456 789}).
 * {@code null} i prázdný řetězec jsou považovány za platné (telefon je nepovinný údaj).
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CzechPhoneValidator.class)
@Documented
public @interface ValidCzechPhone {

    String message() default "Neplatné telefonní číslo – použijte formát +420 XXX XXX XXX";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
