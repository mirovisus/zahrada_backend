package upce.fei.garden.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import upce.fei.garden.validation.rules.ValidCzechPhone;

import java.util.regex.Pattern;

/**
 * Validátor pro {@link ValidCzechPhone} – ověřuje formát {@code +420} a devět číslic,
 * volitelně oddělených mezerami.
 */
public class CzechPhoneValidator implements ConstraintValidator<ValidCzechPhone, String> {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+420\\s?\\d{3}\\s?\\d{3}\\s?\\d{3}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null/prazdny retezec je platny, telefon je nepovinny udaj
        if (value == null || value.isBlank()) {
            return true;
        }
        return PHONE_PATTERN.matcher(value).matches();
    }
}
