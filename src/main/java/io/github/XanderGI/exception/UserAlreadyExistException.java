package io.github.XanderGI.exception;

import lombok.Getter;

@Getter
public class UserAlreadyExistException extends RuntimeException {
    private final String username;

    public UserAlreadyExistException(String message, String username) {
        super(message);
        this.username = username;
    }
}