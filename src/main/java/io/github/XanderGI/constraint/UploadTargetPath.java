package io.github.XanderGI.constraint;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.*;

@Documented
@Pattern(regexp = "^(/|.*[^/].*/)$", message = "Resource path must be non-empty and end with /")
@NotNull(message = "Parameter `path` must be not missing")
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
public @interface UploadTargetPath {
    String message() default "Resource path must be non-empty and end with /";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}