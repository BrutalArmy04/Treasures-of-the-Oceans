package com.oceans;

import java.util.ArrayList;
import java.util.LinkedList;

public class Deck {

    private LinkedList<Card> myDeck = new LinkedList<>();

    public Deck(LinkedList<Card> d) {
        this.myDeck = d;
    }
    public Card dealCard() {
        return myDeck.poll();
    }
    // Look at the top card WITHOUT removing it (needed for the "your card" UI while a human chooses).
    public Card peekTop() {
        return myDeck.peek();
    }
    public void winCards(ArrayList<Card> table) {
        myDeck.addAll(table);
    }
    public boolean emptyDeck() {
        return myDeck.isEmpty();
    }
    public int size() {
        return myDeck.size();
    }
}
