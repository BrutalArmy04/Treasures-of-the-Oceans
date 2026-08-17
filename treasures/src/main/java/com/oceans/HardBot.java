package com.oceans;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

public class HardBot extends Player {

    private LinkedList<Card> memory = new LinkedList<>();
    private int memoryLimit;
    private int baselineSpeed;
    private int baselineSize;
    private int baselineDanger;

    public HardBot(String name, Deck d, int numberOfPlayers, int totalSpeed, int totalSize, int totalDanger, int totalCards) {
        super(name, d);

        double log2P  = Math.log(numberOfPlayers) / Math.log(2);
        double log2C  = Math.log(totalCards) / Math.log(2);
        double log3CP = Math.log((double) totalCards / numberOfPlayers) / Math.log(3);

        this.memoryLimit = (int) (log2P + log2C + log3CP);
        this.baselineSpeed = totalSpeed;
        this.baselineSize = totalSize;
        this.baselineDanger = totalDanger;
    }

    @Override
    public void observeTable(ArrayList<Card> tableCards) {
        memory.addAll(tableCards);
        while (memory.size() > memoryLimit) {
            memory.poll();
        }
    }

    @Override
    public String choosestat() {
        Random rand = new Random();
        int currentSpeed = this.baselineSpeed;
        int currentSize = this.baselineSize;
        int currentDanger = this.baselineDanger;

        for (Card card : memory) {
            currentSpeed  -= card.getSpeed();
            currentSize   -= card.getSize();
            currentDanger -= card.getDanger();
        }

        currentSpeed  = Math.max(0, currentSpeed);
        currentSize   = Math.max(0, currentSize);
        currentDanger = Math.max(0, currentDanger);

        int total = currentSpeed + currentSize + currentDanger;

        if (total <= 0) {
            switch (rand.nextInt(3)) {
                case 0:  return "Speed";
                case 1:  return "Size";
                default: return "Danger";
            }
        }

        int roll = rand.nextInt(total) + 1;
        if (roll <= currentSpeed)            return "Speed";
        else if (roll > total - currentDanger) return "Danger";
        else                                  return "Size";
    }
}
