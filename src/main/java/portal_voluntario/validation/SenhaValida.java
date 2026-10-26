package portal_voluntario.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SenhaValidaValidator.class)
public @interface SenhaValida {
    String message() default "A senha deve conter de 8 a 72 caracteres e no máximo 72 bytes";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
