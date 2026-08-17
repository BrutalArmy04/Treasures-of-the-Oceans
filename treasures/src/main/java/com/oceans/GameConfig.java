package com.oceans;

import java.util.ArrayList;
import java.util.List;

// The request body that starts a game: how many cards, and who's playing.
public class GameConfig {
    private int deckSize;
    private List<PlayerConfig> players = new ArrayList<>();

    public int getDeckSize() { return deckSize; }
    public void setDeckSize(int deckSize) { this.deckSize = deckSize; }
    public List<PlayerConfig> getPlayers() { return players; }
    public void setPlayers(List<PlayerConfig> players) { this.players = players; }
}
