package com.oceans;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps the engine's unchecked exceptions onto HTTP status codes, so a caller mistake
 * reads as a 4xx instead of a 500 stack trace.
 *
 *   GameNotFoundException    -> 404  unknown or deleted game id
 *   IllegalArgumentException -> 400  bad config, or a stat that isn't Speed/Size/Danger
 *   IllegalStateException    -> 409  right game, wrong moment (e.g. choose() before advance())
 *
 * There is no catch-all handler on purpose: anything unforeseen stays a 500 so it shows up
 * as a bug rather than being quietly dressed up as a client error.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(GameNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleConflict(IllegalStateException ex) {
        return build(HttpStatus.CONFLICT, ex);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, RuntimeException ex) {
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), status.getReasonPhrase(), ex.getMessage()));
    }
}
