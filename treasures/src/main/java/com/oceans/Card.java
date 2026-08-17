package com.oceans;

public class Card{
    private String name;
    private int id, speed, size, danger;

    private static int idCounter = 0 ;

    public Card(String name, int speed, int size, int danger)
    {
        this.id = idCounter++;
        this.speed = speed;
        this.size = size;
        this.danger = danger;
        this.name = name;
    }

    public String getName()
    {return this.name;}
    public int getSpeed()
    {return this.speed;}
     public int getSize()
    {return this.size;}
     public int getDanger()
    {return this.danger;}
    public int getId()
    {return id;}

    @Override
    public String toString() {

        String border = "+------------------------+";        
        StringBuilder cardArt = new StringBuilder();
        cardArt.append(border).append("\n");
        cardArt.append(String.format("| %-22s |\n", this.getName()));
        cardArt.append(String.format("| ID: %-18d |\n", this.getId()));
        cardArt.append(border).append("\n");
        cardArt.append(String.format("| Speed:  %-14d |\n", this.getSpeed()));
        cardArt.append(String.format("| Size:   %-14d |\n", this.getSize()));
        cardArt.append(String.format("| Danger: %-14d |\n", this.getDanger()));
        cardArt.append(border);
        
        return cardArt.toString();
    }
}