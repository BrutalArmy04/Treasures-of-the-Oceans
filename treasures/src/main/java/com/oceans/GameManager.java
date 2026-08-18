package com.oceans;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Owns every in-flight game.
 *
 * GameEngine holds mutable per-game state, so it cannot be a bean itself -- one shared
 * instance would mean every visitor playing the same game. Instead this service is the
 * singleton, and it hands out one engine per game id. The map is concurrent because
 * Tomcat serves requests from a pool of threads.
 *
 * Games live until DELETEd; nothing evicts them yet.
 */
@Service
public class GameManager {

    private final Map<String, GameEngine> games = new ConcurrentHashMap<>();

    /** Build and set up a game, then register it. Returns the id the client uses from here on. */
    public String createGame(GameConfig config) {
        GameEngine engine = new GameEngine();
        engine.setup(config);   // validates first -- a rejected config never lands in the map
        String id = UUID.randomUUID().toString();
        games.put(id, engine);
        return id;
    }

    public GameEngine get(String id) {
        GameEngine engine = games.get(id);
        if (engine == null) {
            throw new GameNotFoundException(id);
        }
        return engine;
    }

    public void remove(String id) {
        games.remove(id);
    }
}
