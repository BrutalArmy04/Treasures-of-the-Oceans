package com.oceans;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The HTTP face of the engine's state machine. Each endpoint is one step:
 *
 *   POST   /api/games              -> setup(config), returns the id + opening state
 *   GET    /api/games/{id}         -> the current snapshot
 *   POST   /api/games/{id}/advance -> one round (bots resolve; a human turn parks at AWAITING_PLAYER_CHOICE)
 *   POST   /api/games/{id}/choose  -> supply the human's stat, resolving that round
 *   DELETE /api/games/{id}         -> forget the game
 *
 * The client drives the loop by reading `status` off each response, exactly as
 * EngineSmokeTest drives it in-process. Nothing here loops on the server's behalf.
 *
 * Bad input and out-of-order calls surface as the engine's own IllegalArgumentException /
 * IllegalStateException; ApiExceptionHandler turns those into 400 / 409.
 */
@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameManager manager;

    public GameController(GameManager manager) {
        this.manager = manager;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateGameResponse create(@RequestBody GameConfig config) {
        String id = manager.createGame(config);
        return new CreateGameResponse(id, manager.get(id).getState());
    }

    @GetMapping("/{id}")
    public GameStateView state(@PathVariable String id) {
        return manager.get(id).getState();
    }

    /**
     * One step only -- deliberately not a loop. advance() returns void, so the round it
     * just played is read back off the snapshot as `state.lastRound`.
     */
    @PostMapping("/{id}/advance")
    public GameStateView advance(@PathVariable String id) {
        GameEngine engine = manager.get(id);
        engine.advance();
        return engine.getState();
    }

    @PostMapping("/{id}/choose")
    public GameStateView choose(@PathVariable String id, @RequestBody ChooseRequest request) {
        GameEngine engine = manager.get(id);
        engine.chooseStat(request.stat());
        return engine.getState();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        manager.remove(id);
    }
}
