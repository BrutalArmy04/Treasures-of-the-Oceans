import java.util.Random;

public class HardBot extends Player{

    public HardBot(String name, Deck d)
    {super(name, d);}

    @Override
    public String choosestat()
    {
        Random rand = new Random();
        switch ((rand.nextInt(3) + 1))
        {
            case 1:
                return "Speed";
            case 2:
                return "Size";
            default :
                return "Danger";
        }
        
    }
}