package upce.fei.garden.validation.rules;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import upce.fei.garden.validation.validator.FutureOrTodayValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Vlastní validační pravidlo: anotované {@link java.time.LocalDate} nesmí ležet v minulosti –
 * dnešní datum i libovolné budoucí datum je platné. {@code null} hodnota je považována za platnou
 * (o povinnosti pole rozhoduje samostatná anotace {@code @NotNull}).
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FutureOrTodayValidator.class)
@Documented
public @interface FutureOrToday {

    String message() default "Datum nesmí být v minulosti";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
