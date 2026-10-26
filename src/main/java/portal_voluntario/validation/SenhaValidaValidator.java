package portal_voluntario.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class SenhaValidaValidator implements ConstraintValidator<SenhaValida, String> {
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && !value.isBlank() && value.length() >= 8 && value.length() <= 72
                && value.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
