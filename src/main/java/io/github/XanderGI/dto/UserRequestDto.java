package io.github.XanderGI.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request with credentials required to create a new user account")
public record UserRequestDto(

        @Schema(
                description = "User's username",
                example = "xander"
        )
        @Pattern(regexp = "^[a-zA-Z0-9]+[a-zA-Z_0-9]*[a-zA-Z0-9]+$",
                message = "Invalid format. Minimum 3 characters required. Must start and end with a letter or digit")
        @NotBlank(message = "Username cannot be empty")
        @Size(min = 5, max = 20, message = "Username must be between {min} and {max} characters long")
        String username,

        @Schema(
                description = "Password used for registration or authorization",
                example = "qwerty123"
        )
        @Pattern(regexp = "^[a-zA-Z0-9!@#$%^&*(),.?\\\":{}|<>[\\\\]/`~+=-_';]*$",
                message = "Password contains invalid characters. Only alphanumeric and standard special characters are allowed")
        @NotBlank(message = "Password cannot be empty")
        @Size(min = 5, max = 64, message = "Password must be between {min} and {max} characters long")
        String password
) {
}