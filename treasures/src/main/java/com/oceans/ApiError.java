package com.oceans;

// One consistent error shape for every handled failure, so the client can branch on
// status without special-casing each endpoint.
public record ApiError(int status, String error, String message) {}
