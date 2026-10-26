package portal_voluntario.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DocumentoUrlValidator.class)
public @interface DocumentoUrl {
    String message() default "Informe um link HTTP ou HTTPS sem credenciais";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
