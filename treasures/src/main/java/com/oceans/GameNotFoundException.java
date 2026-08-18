package com.oceans;

// Thrown when a request names a game id the manager has never handed out, or one
// that has since been deleted. ApiExceptionHandler maps this to 404.
public class GameNotFoundException extends RuntimeException {
    public GameNotFoundException(String id) {
        super("No game with id: " + id);
    }
}
