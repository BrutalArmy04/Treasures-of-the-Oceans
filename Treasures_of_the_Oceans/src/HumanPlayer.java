import java.util.Scanner;

public class HumanPlayer extends Player{
    
    public HumanPlayer(String name, Deck d) {
        super(name, d);
    }
    @Override
    public String choosestat() {
        Scanner userInput = new Scanner(System.in); 
            while (true)
            {
                try
                {
                    System.out.println("Choose your stat: 1 for Speed, 2 for Size, 3 for Danger");
                    int choice = userInput.nextInt();
                    switch (choice) {
                        case 1:
                            return "Speed";
                        case 2:
                            return "Size";
                        case 3:
                            return "Danger";
                        default:
                            System.out.println("Invalid choice, try again!");
                    }
                }
                catch(Exception e)
                {
                    userInput.nextLine();
                }
            }
        }
    }



