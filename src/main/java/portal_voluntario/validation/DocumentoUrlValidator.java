package portal_voluntario.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.URI;

public class DocumentoUrlValidator implements ConstraintValidator<DocumentoUrl, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true; // @NotBlank handles missing values.
        try {
            URI uri = URI.create(value.trim());
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getRawUserInfo() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
