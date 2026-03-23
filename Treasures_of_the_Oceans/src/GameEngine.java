import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

public class GameEngine {
    private ArrayList<Player> activePlayers;
    HashMap<Player, Card> tableMap;
    private int turnPlayerIndex;
    ArrayList<Card> deck = new ArrayList<>();

    public GameEngine() {
        this.activePlayers = new ArrayList<>();
        this.tableMap = new HashMap<Player, Card>();
        this.turnPlayerIndex = 0; 
    }
    private int maxNumberOfPlayers = 4;

    public int getMenuChoice()
    {
        Scanner userInput = new Scanner(System.in); 
         while (true)
            { 
                try
                {
                    System.out.println("Choose the number of players (minimum 2, maximum " + maxNumberOfPlayers + ")");
                    int choice = userInput.nextInt();
                    if (choice > 1 && choice <=(maxNumberOfPlayers))
                        return choice;
                    else
                        System.out.println("Invalid choice, try again!");
                }
                catch(Exception e)
                {
                    userInput.nextLine();
                }
                
            }
    }

    public void setup() {
        int numberOfPlayers = getMenuChoice();
        deck = DeckFactory.returnDeck();
        Collections.shuffle(deck);
        int cardsPerPlayer = deck.size() / numberOfPlayers;
        ArrayList<LinkedList<Card>> tablePiles = new ArrayList<>();

        for (int i = 0; i < numberOfPlayers; i++)
            tablePiles.add(new LinkedList<>());

        for (int i = 0; i < (cardsPerPlayer * numberOfPlayers); i++) {
            tablePiles.get(i % numberOfPlayers).add(deck.get(i));
        }

        Scanner setupScanner = new Scanner(System.in);

        for (int i = 0; i < numberOfPlayers; i++) {
            System.out.println("\nWho is sitting in Seat " + (i + 1) + "?");
            System.out.println("1. Human Player");
            System.out.println("2. Easy Bot");
            System.out.println("3. Medium Bot");
            System.out.println("4. Hard Bot");
            
            int choice = 0;
            while (true) {
                try {
                    choice = setupScanner.nextInt();
                    if (choice >= 1 && choice <= 4) {
                        break;
                    }
                } catch (Exception e) {
                    setupScanner.nextLine();
                }
                System.out.println("Invalid choice. Please enter 1, 2, 3, or 4:");
            }

            System.out.println("You may give this player a name.");
            setupScanner.nextLine();
            String name = setupScanner.nextLine();
            if (name.trim().isEmpty()) {
            name = "Player " + (i + 1);
    }
            Deck assignedDeck = new Deck(tablePiles.get(i));


            switch (choice) {
                case 1:
                    activePlayers.add(new HumanPlayer(name, assignedDeck));
                    break;
                case 2:
                    activePlayers.add(new EasyBot(name, assignedDeck));
                    break;
                case 3:
                    activePlayers.add(new MediumBot(name, assignedDeck));
                    break;
                case 4:
                    activePlayers.add(new HardBot(name, assignedDeck));
                    break;
            }
        }

        System.out.println("\nSetup complete! Let the battle begin.");
    }

    public void startGame()
    {
        System.out.println("Start game");
        
        while (activePlayers.size() > 1) {
            playRound();
            removeEliminated();
            
            if (activePlayers.size() > 1) {
                promptNextRound(); 
            }
        }
        System.out.println("\n==================================");
        System.out.println("           Game Over!             ");
        System.out.println("==================================");
        
        if (activePlayers.size() == 1) {
            Player grandChampion = activePlayers.get(0);
            System.out.println("The Winner is: " + grandChampion.getName() + "!");
        } else {
            System.out.println("Everyone was eliminated! It's a total draw!");
        }
        
    }

    private void removeEliminated() {
        Player currentTurnHolder = activePlayers.get(turnPlayerIndex);
        for (int i = activePlayers.size() - 1; i >= 0; i--) {
            if (!activePlayers.get(i).hasCardsLeft()) {
                System.out.println(activePlayers.get(i).getName() + " has run out of cards and is eliminated!");
                activePlayers.remove(i);
            }
        }
        turnPlayerIndex = activePlayers.indexOf(currentTurnHolder);
        if (turnPlayerIndex == -1) 
            turnPlayerIndex = 0; 
        
    }

    private void promptNextRound() {
    Scanner pauseScanner = new Scanner(System.in);
    System.out.println("\n[Press ENTER to start the next round...]");
    pauseScanner.nextLine(); 
}

    private void playRound()
    {
        deck.clear();
        tableMap.clear();
        Player currentPlayer = activePlayers.get(turnPlayerIndex);
        System.out.println("\nIt is " + currentPlayer.getName() + "'s turn!");
        String roundStat = currentPlayer.choosestat();
        System.out.println(currentPlayer.getName() + " chose: " + roundStat);

        evaluateCards(activePlayers, roundStat);
    }
    
    private void evaluateCards(ArrayList<Player> participants, String roundStat) {
        
        tableMap.clear(); 

        for (Player player : participants) {
            if (player.hasCardsLeft()) { 
                Card playCard = player.playCard();
                System.out.println(player.getName() + " played:\n" + playCard);
                
                tableMap.put(player, playCard);
                deck.add(playCard); 
            }
        }

        int maxStat = 0;
        ArrayList<Player> winningPlayers = new ArrayList<>();

        for (Player player : tableMap.keySet()) {
            Card currentCard = tableMap.get(player);
            int currentStat = 0;

            if ("Speed".equals(roundStat)) {
                currentStat = currentCard.getSpeed();
            } else if ("Size".equals(roundStat)) {
                currentStat = currentCard.getSize();
            } else {
                currentStat = currentCard.getDanger();
            }

            if (maxStat < currentStat) {
                maxStat = currentStat;
                winningPlayers.clear();
                winningPlayers.add(player);
            } else if (maxStat == currentStat) {
                winningPlayers.add(player);
            }
        }

        if (winningPlayers.size() == 1) {
            Player winner = winningPlayers.get(0);
            System.out.println("*** " + winner.getName() + " wins the round! ***");
        
            winner.winCards(deck); 
            turnPlayerIndex = activePlayers.indexOf(winner); 
            
        } else if (winningPlayers.size() > 1) {
            System.out.println("--- TIE BREAKER! ---");
            evaluateCards(winningPlayers, roundStat);
        }
    }
}
