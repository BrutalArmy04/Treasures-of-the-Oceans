package com.oceans;

// The engine is a small state machine. The controller reads this to know what to do next.
public enum GameStatus {
    NOT_STARTED,             // setup() not called yet
    AWAITING_NEXT_ROUND,     // a round is ready to be played for the current turn holder -> call advance()
    AWAITING_PLAYER_CHOICE,  // it's a human's turn; waiting for a stat via chooseStat(...)
    GAME_OVER                // a winner (or draw) has been decided
}
