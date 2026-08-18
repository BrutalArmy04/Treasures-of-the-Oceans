package com.oceans;

// Body of POST /api/games/{id}/choose. The engine takes the stat as a raw String
// ("Speed" | "Size" | "Danger", case-insensitive) and normalises it itself.
public record ChooseRequest(String stat) {}
