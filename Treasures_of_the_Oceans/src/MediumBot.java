import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

public class MediumBot extends Player{

    private LinkedList<Card> memory = new LinkedList<>();
    private int memoryLimit;
    private int baselineSpeed;
    private int baselineSize;
    private int baselineDanger;


    public MediumBot(String name, Deck d, int numberOfPlayers, int totalSpeed, int totalSize, int totalDanger)
    {super(name, d);
        this.memoryLimit = numberOfPlayers;
        this.baselineSpeed = totalSpeed;
        this.baselineSize = totalSize;
        this.baselineDanger = totalDanger;

    }

    @Override
    public void observeTable(ArrayList<Card> tableCards)
    {
        
        memory.addAll(tableCards);

        while (memory.size() > memoryLimit)
        {
            memory.poll();
        }
    }

    @Override
    public String choosestat()
    {
        Random rand = new Random();
        int currentSpeed = this.baselineSpeed;
        int currentSize = this.baselineSize;
        int currentDanger = this.baselineDanger;

        for (Card card : memory)
        {
            currentSpeed -= card.getSpeed();
            currentSize -= card.getSize();
            currentDanger -= card.getDanger();
        }

        int total = currentDanger + currentSize + currentSpeed;

        int roll = rand.nextInt(total) + 1;
        
        if (roll <= currentSpeed)
            return "Speed";
        else if (roll > total - currentDanger)
            return "Danger";
        else return "Size";
    }
}
