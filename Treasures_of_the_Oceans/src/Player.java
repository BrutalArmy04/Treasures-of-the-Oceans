import java.util.ArrayList;

public abstract class Player{

    private String name;
    private Deck myDeck;


    public Player(String name, Deck d)
    {
        this.name = name;
        this.myDeck = d;
    }
    public String getName()
    {
        return this.name;
    }
    public Card playCard()
    {
        return this.myDeck.dealCard();
    }
    public void winCards(ArrayList<Card> table)
    {
        this.myDeck.winCards(table);
    }
    public boolean hasCardsLeft()
    {
        return !this.myDeck.emptyDeck();
    }
    public abstract String choosestat();
    public void observeTable(ArrayList<Card> tableCards) { }
}