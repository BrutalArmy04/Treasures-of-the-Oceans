import java.util.Random;

public class EasyBot extends Player{

    public EasyBot(String name, Deck d)
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