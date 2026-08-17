package com.oceans;

import java.util.ArrayList;
import java.util.List;

// The serializable snapshot the controller returns each request.
// Honours blind play: opponents' upcoming cards are never included.
public class GameStateView {
    public GameStatus status;
    public String turnHolder;          // whose turn it is to choose
    public String winnerName;          // set when status == GAME_OVER
    public RoundResult lastRound;      // the previous round's revealed cards (safe), or null
    public List<PlayerView> players = new ArrayList<>();

    public static class PlayerView {
        public String name;
        public boolean human;
        public int cardsRemaining;
        // Only ever set for the human who is currently being asked to choose — their OWN top card.
        public RoundResult.Reveal topCard;
    }
}
