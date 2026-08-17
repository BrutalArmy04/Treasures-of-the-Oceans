package com.oceans;

public class EngineSmokeTest {
    public static void main(String[] args) {
        int games = 2000, easy = 0, medium = 0, hard = 0, draws = 0, longest = 0;

        for (int g = 0; g < games; g++) {
            GameConfig cfg = new GameConfig();
            cfg.setDeckSize(30);
            cfg.getPlayers().add(new PlayerConfig(PlayerType.EASY_BOT,   "Easy Bot"));
            cfg.getPlayers().add(new PlayerConfig(PlayerType.MEDIUM_BOT, "Medium Bot"));
            cfg.getPlayers().add(new PlayerConfig(PlayerType.HARD_BOT,   "Hard Bot"));

            GameEngine e = new GameEngine();
            e.setup(cfg);

            int guard = 0;
            while (e.getStatus() != GameStatus.GAME_OVER) {
                e.advance();                 // all bots -> never AWAITING_PLAYER_CHOICE
                if (++guard > 200000) { System.out.println("RUNAWAY GAME"); break; }
            }
            longest = Math.max(longest, guard);
            String w = e.getState().winnerName;
            if (w == null) draws++;
            else if (w.equals("Easy Bot")) easy++;
            else if (w.equals("Medium Bot")) medium++;
            else hard++;
        }

        System.out.println("Games: " + games + " | longest round-count: " + longest);
        System.out.println("Easy  : " + easy   + " (" + easy*100/games + "%)");
        System.out.println("Medium: " + medium + " (" + medium*100/games + "%)");
        System.out.println("Hard  : " + hard   + " (" + hard*100/games + "%)");
        System.out.println("Draws : " + draws);
    }
}
