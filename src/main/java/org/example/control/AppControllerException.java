package org.example.control;

public class AppControllerException extends RuntimeException {
    public AppControllerException(String message) {
        super(message);
    }

    public AppControllerException(String message, Throwable cause) {
        super(message, cause);
    }
}
