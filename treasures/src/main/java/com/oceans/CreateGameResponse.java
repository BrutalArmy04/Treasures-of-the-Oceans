package com.oceans;

// The reply to POST /api/games: the id to use for every later call, plus the opening
// state, so the client can render the table without a second round-trip.
public record CreateGameResponse(String gameId, GameStateView state) {}
