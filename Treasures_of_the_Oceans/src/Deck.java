import java.util.ArrayList;
import java.util.LinkedList;

public class Deck {
    
    
    private LinkedList<Card> myDeck = new LinkedList<>(); 
    public Deck(LinkedList <Card> d)
    {
        this.myDeck = d;
    }
    public Card dealCard()
    {
        return myDeck.poll();
    }

    public void winCards(ArrayList<Card> table)
    {
            myDeck.addAll(table);
    }
    public boolean emptyDeck()
    {
        return myDeck.isEmpty();
    }
}
