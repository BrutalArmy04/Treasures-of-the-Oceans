import java.util.ArrayList;
import java.util.Collections;


public class DeckFactory
{

    private static ArrayList<Card> masterList = new ArrayList<>(); 

    private static void initializeCards()

    {
    masterList.clear();
    
    masterList.add(new Card("Great White Shark", 55, 90, 95));
    masterList.add(new Card("Whale Shark", 20, 100, 5));
    masterList.add(new Card("Hammerhead Shark", 60, 85, 80));
    masterList.add(new Card("Giant Squid", 70, 95, 90));
    masterList.add(new Card("Sailfish", 100, 60, 30));
    masterList.add(new Card("Black Marlin", 95, 75, 40));
    masterList.add(new Card("Swordfish", 90, 70, 65));
    masterList.add(new Card("Barracuda", 85, 40, 75));
    masterList.add(new Card("Pufferfish", 25, 15, 85));
    masterList.add(new Card("Piranha", 65, 10, 90));
    masterList.add(new Card("Lionfish", 30, 20, 85));
    masterList.add(new Card("Blue-Ringed Octopus", 40, 5, 100)); 
    masterList.add(new Card("Electric Eel", 45, 50, 95));
    masterList.add(new Card("Moray Eel", 50, 45, 75));
    masterList.add(new Card("Manta Ray", 40, 85, 15));
    masterList.add(new Card("Tuna", 75, 55, 20));
    masterList.add(new Card("Clownfish", 45, 10, 5));
    masterList.add(new Card("Seahorse", 15, 5, 2));
    masterList.add(new Card("Flounder", 35, 25, 10));
    masterList.add(new Card("Blobfish", 5, 30, 1));

    }
    public static ArrayList<Card> returnDeck()
{
    initializeCards();
    Collections.shuffle(masterList);
    return masterList;
}

    }
