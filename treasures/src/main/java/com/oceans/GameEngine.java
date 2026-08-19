package com.oceans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Stateful, non-blocking game engine.
 *
 * The old design ran one blocking while-loop that read Scanner input mid-loop. A REST
 * server can't block waiting for a human, so the loop is inverted into a state machine:
 * the controller drives the engine one step at a time and the engine reports, via
 * {@link GameStatus}, whether it needs a human choice, is ready to play a bot round, or
 * is finished.
 *
 *   setup(config)   -> AWAITING_NEXT_ROUND
 *   advance()       -> plays the current round if the chooser is a bot (stays AWAITING_NEXT_ROUND
 *                      or becomes GAME_OVER), or pauses at AWAITING_PLAYER_CHOICE for a human
 *   chooseStat(s)   -> applies a human's stat, resolves the round, then AWAITING_NEXT_ROUND / GAME_OVER
 *   getState()      -> a blind-play-safe snapshot for the client
 *
 * No Scanner, no System.out. Everything the UI needs is in getState().
 */
public class GameEngine {

    private final ArrayList<Player> activePlayers = new ArrayList<>();
    private final HashMap<Player, Card> tableMap = new HashMap<>();
    private final ArrayList<Card> pile = new ArrayList<>();   // the cards staked this round
    private int turnPlayerIndex = 0;

    private GameStatus status = GameStatus.NOT_STARTED;
    private RoundResult lastRound = null;
    private String winnerName = null;

    // ------------------------------------------------------------------ setup

    public void setup(GameConfig config) {
        if (config == null || config.getPlayers() == null || config.getPlayers().size() < 2) {
            throw new IllegalArgumentException("A game needs at least 2 players.");
        }
        int numberOfPlayers = config.getPlayers().size();

        ArrayList<Card> masterDeck = DeckFactory.returnDeck();
        Collections.shuffle(masterDeck);

        int requested = config.getDeckSize();
        if (requested < numberOfPlayers) {
            throw new IllegalArgumentException(
                "deckSize (" + requested + ") must be at least the number of players (" + numberOfPlayers + ").");
        }
        int deckSize = Math.min(requested, masterDeck.size());
        ArrayList<Card> chosen = new ArrayList<>(masterDeck.subList(0, deckSize));

        int cardsPerPlayer = chosen.size() / numberOfPlayers;
        int totalCardsDealt = cardsPerPlayer * numberOfPlayers;   // remainder is discarded

        ArrayList<LinkedList<Card>> hands = new ArrayList<>();
        for (int i = 0; i < numberOfPlayers; i++) hands.add(new LinkedList<>());

        int totalSpeed = 0, totalSize = 0, totalDanger = 0;
        for (int i = 0; i < totalCardsDealt; i++) {
            Card c = chosen.get(i);
            hands.get(i % numberOfPlayers).add(c);
            totalSpeed  += c.getSpeed();
            totalSize   += c.getSize();
            totalDanger += c.getDanger();
        }

        for (int i = 0; i < numberOfPlayers; i++) {
            PlayerConfig pc = config.getPlayers().get(i);
            String name = (pc.getName() == null || pc.getName().trim().isEmpty())
                    ? "Player " + (i + 1) : pc.getName().trim();
            Deck hand = new Deck(hands.get(i));

            switch (pc.getType()) {
                case HUMAN:      activePlayers.add(new HumanPlayer(name, hand)); break;
                case EASY_BOT:   activePlayers.add(new EasyBot(name, hand)); break;
                case MEDIUM_BOT: activePlayers.add(new MediumBot(name, hand, numberOfPlayers, totalSpeed, totalSize, totalDanger)); break;
                case HARD_BOT:   activePlayers.add(new HardBot(name, hand, numberOfPlayers, totalSpeed, totalSize, totalDanger, totalCardsDealt)); break;
                default: throw new IllegalArgumentException("Unknown player type: " + pc.getType());
            }
        }

        this.turnPlayerIndex = 0;
        this.status = GameStatus.AWAITING_NEXT_ROUND;
    }

    // ---------------------------------------------------------------- stepping

    /** Advance one round for the current turn holder. Bot rounds resolve immediately;
     *  a human turn pauses at AWAITING_PLAYER_CHOICE for {@link #chooseStat}. */
    public void advance() {
        requireStatus(GameStatus.AWAITING_NEXT_ROUND);
        Player chooser = activePlayers.get(turnPlayerIndex);
        if (chooser.isHuman()) {
            status = GameStatus.AWAITING_PLAYER_CHOICE;
            return;
        }
        resolveRound(chooser.choosestat(), chooser);   // bots pick their own stat (never throws)
    }

    /** Apply the human turn holder's stat and resolve the round. */
    public void chooseStat(String stat) {
        requireStatus(GameStatus.AWAITING_PLAYER_CHOICE);
        Player chooser = activePlayers.get(turnPlayerIndex);
        resolveRound(normaliseStat(stat), chooser);
    }

    // ------------------------------------------------------------- round logic

    private void resolveRound(String stat, Player chooser) {
        pile.clear();
        tableMap.clear();

        RoundResult result = new RoundResult();
        result.chooserName = chooser.getName();
        result.stat = stat;

        evaluate(new ArrayList<>(activePlayers), stat, result);
        this.lastRound = result;

        removeEliminated();

        if (activePlayers.size() <= 1) {
            status = GameStatus.GAME_OVER;
            winnerName = activePlayers.isEmpty() ? null : activePlayers.get(0).getName();
        } else {
            status = GameStatus.AWAITING_NEXT_ROUND;
        }
    }

    // Reveal, compare, award. Ties trigger a sudden-death replay on the SAME stat (Top Trumps "war").
    private void evaluate(ArrayList<Player> participants, String stat, RoundResult result) {
        tableMap.clear();

        for (Player p : participants) {
            if (p.hasCardsLeft()) {
                Card played = p.playCard();
                tableMap.put(p, played);
                pile.add(played);
                result.reveals.add(new RoundResult.Reveal(p.getName(), played));
            }
        }

        int maxStat = -1;   // -1 (not 0) so an all-zero stat still yields a single winner
        ArrayList<Player> winners = new ArrayList<>();
        for (Player p : tableMap.keySet()) {
            int v = statValue(tableMap.get(p), stat);
            if (v > maxStat) {
                maxStat = v;
                winners.clear();
                winners.add(p);
            } else if (v == maxStat) {
                winners.add(p);
            }
        }

        if (winners.size() == 1) {
            for (Player p : activePlayers) p.observeTable(pile);
            Player winner = winners.get(0);
            winner.winCards(pile);
            turnPlayerIndex = activePlayers.indexOf(winner);
            result.winnerName = winner.getName();
        } else if (winners.size() > 1) {
            result.war = true;
            evaluate(winners, stat, result);   // tied players replay their next card
        }
        // winners empty => nobody could play; removeEliminated() cleans up.
    }

    private void removeEliminated() {
        if (activePlayers.isEmpty()) return;
        Player currentHolder = activePlayers.get(turnPlayerIndex);
        for (int i = activePlayers.size() - 1; i >= 0; i--) {
            if (!activePlayers.get(i).hasCardsLeft()) {
                activePlayers.remove(i);
            }
        }
        int idx = activePlayers.indexOf(currentHolder);
        turnPlayerIndex = (idx == -1) ? 0 : idx;
    }

    // --------------------------------------------------------------- reporting

    /** A snapshot for the client. Opponents' upcoming cards are never exposed (blind play). */
    public GameStateView getState() {
        GameStateView view = new GameStateView();
        view.status = status;
        view.winnerName = winnerName;
        view.lastRound = lastRound;
        view.turnHolder = activePlayers.isEmpty() ? null : activePlayers.get(turnPlayerIndex).getName();

        for (int i = 0; i < activePlayers.size(); i++) {
            Player p = activePlayers.get(i);
            GameStateView.PlayerView pv = new GameStateView.PlayerView();
            pv.name = p.getName();
            pv.human = p.isHuman();
            pv.cardsRemaining = p.cardsRemaining();
            view.players.add(pv);
        }
        return view;
    }

    public GameStatus getStatus() { return status; }

    // ----------------------------------------------------------------- helpers

    private int statValue(Card c, String stat) {
        switch (stat) {
            case "Speed":  return c.getSpeed();
            case "Size":   return c.getSize();
            case "Danger": return c.getDanger();
            default: throw new IllegalArgumentException("Unknown stat: " + stat);
        }
    }

    private String normaliseStat(String stat) {
        if (stat == null) throw new IllegalArgumentException("stat is required");
        switch (stat.trim().toLowerCase()) {
            case "speed":  return "Speed";
            case "size":   return "Size";
            case "danger": return "Danger";
            default: throw new IllegalArgumentException("stat must be Speed, Size or Danger (got '" + stat + "')");
        }
    }

    private void requireStatus(GameStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Expected status " + expected + " but was " + status + ".");
        }
    }
}
