package com.oceans;

import java.util.ArrayList;
import java.util.List;

// A snapshot of the round that just resolved. Safe to reveal fully because the round is over.
public class RoundResult {
    public String chooserName;         // who picked the stat
    public String stat;                // "Speed" | "Size" | "Danger"
    public String winnerName;          // null only in the (astronomically unlikely) unresolved case
    public boolean war;                // true if a tie forced a sudden-death replay
    public List<Reveal> reveals = new ArrayList<>();

    public static class Reveal {
        public String playerName;
        public String cardName;
        public int speed, size, danger;
        public Reveal(String playerName, Card c) {
            this.playerName = playerName;
            this.cardName = c.getName();
            this.speed = c.getSpeed();
            this.size = c.getSize();
            this.danger = c.getDanger();
        }
    }
}
