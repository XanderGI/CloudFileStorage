package io.github.XanderGI.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRequestDto(
        @Pattern(regexp = "^[a-zA-Z0-9]+[a-zA-Z_0-9]*[a-zA-Z0-9]+$", message = "Invalid format. Minimum 3 characters required. Must start and end with a letter or digit")
        @NotBlank(message = "Username cannot be empty")
        @Size(min = 5, max = 20, message = "Username must be between 5 and 20 characters long")
        String username,

        @Pattern(regexp = "^[a-zA-Z0-9!@#$%^&*(),.?\\\":{}|<>[\\\\]/`~+=-_';]*$", message = "Password contains invalid characters. Only alphanumeric and standard special characters are allowed")
        @NotBlank(message = "Password cannot be empty")
        @Size(min = 5, max = 64, message = "Password must be between 5 and 64 characters long")
        String password
) {
}