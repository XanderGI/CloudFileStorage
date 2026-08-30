package io.github.XanderGI.constraint;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.*;

@Documented
@Pattern(regexp = "^(.*[^/].*/?)$", message = "Path must not be root and must be a valid resource path")
@NotNull(message = "Parameters `from` and `to` must be not missing")
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
public @interface NonRootResourcePath {
    String message() default "Path must not be root and must be a valid resource path";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}