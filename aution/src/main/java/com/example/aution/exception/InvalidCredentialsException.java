
package com.example.aution.exception;

/**
 * Thrown when login credentials are incorrect (wrong username or password).
 * Maps to HTTP 401 UNAUTHORIZED.
 *
 * Important: the message is intentionally vague — never tell the client
 * whether the username or the password was wrong. That leaks user existence.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}