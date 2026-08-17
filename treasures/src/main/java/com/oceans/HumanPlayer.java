package com.oceans;

public class HumanPlayer extends Player {

    public HumanPlayer(String name, Deck d) {
        super(name, d);
    }

    @Override
    public boolean isHuman() { return true; }

    @Override
    public String choosestat() {
        // In the web build a human's stat choice arrives via the API and is applied
        // through GameEngine.chooseStat(...). The engine must never call this.
        throw new UnsupportedOperationException(
            "Human choices come through the API, not this method.");
    }
}
