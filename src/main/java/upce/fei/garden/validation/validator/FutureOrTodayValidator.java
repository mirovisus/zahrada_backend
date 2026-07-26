package upce.fei.garden.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import upce.fei.garden.validation.rules.FutureOrToday;

import java.time.LocalDate;

/**
 * Validátor pro {@link FutureOrToday} – ověřuje, že datum není starší než dnešní den.
 */
public class FutureOrTodayValidator implements ConstraintValidator<FutureOrToday, LocalDate> {

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        // null se nechává na @NotNull, aby šlo obě anotace nezávisle kombinovat
        if (value == null) {
            return true;
        }
        return !value.isBefore(LocalDate.now());
    }
}
