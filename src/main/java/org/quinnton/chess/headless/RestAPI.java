package org.quinnton.chess.headless;

import org.quinnton.chess.core.GameRules;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class RestAPI {
    public HeadlessConfig cfg;
    public GameService game;

    public RestAPI(HeadlessConfig cfg, GameService game) {
        this.cfg = cfg;
        this.game = game;
    }

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, World!\n";
    }

    @GetMapping("/set-depth/{depth}")
    public ResponseEntity<String> setDepth(@PathVariable int depth) {
        cfg.setDepth(depth);
        return ResponseEntity.ok("Depth is: " + depth);
    }

    @GetMapping("/set-budgetmillis/{budgetMillis}")
    public ResponseEntity<String> setBudgetMillis(@PathVariable long budgetMillis) {
        cfg.setBudgetMillis(budgetMillis);
        return ResponseEntity.ok("Budget millis is: " + budgetMillis);
    }

    @GetMapping("/set-maxPlies/{maxPlies}")
    public ResponseEntity<String> setMaxPlies(@PathVariable int maxPlies) {
        cfg.setMaxPlies(maxPlies);
        return ResponseEntity.ok("Max plies is: " + maxPlies);
    }

    @GetMapping("/set-color/{isWhite}")
    public ResponseEntity<String> setColor(@PathVariable boolean isWhite) {
        cfg.setIsWhite(isWhite);
        return ResponseEntity.ok("Color Is White: " + isWhite);
    }

    @GetMapping("/set-logMoves/{logMoves}")
    public ResponseEntity<String> setLogMoves(@PathVariable boolean logMoves) {
        cfg.setLogMoves(logMoves);
        return ResponseEntity.ok("Logging moves is: " + logMoves);
    }

    @GetMapping("/set-FEN/")
    public ResponseEntity<String> setFEN(@RequestParam String FEN) {
        cfg.setFEN(FEN);
        return ResponseEntity.ok("FEN is: " + FEN);
    }

    /** Rebuild the board from the FEN currently in config. */
    @GetMapping("/new-game")
    public ResponseEntity<String> newGame() {
        game.newGame();
        return ResponseEntity.ok("New game from FEN: " + cfg.getFEN());
    }

    @GetMapping("/get-checkmate")
    public ResponseEntity<String> getCheckmate() {
        if (game.getBoard().isGameOver()) {
            return ResponseEntity.ok("checkmate");
        }
        return ResponseEntity.ok("Game not over");
    }

    /**
     * Makes a move off of the FEN, and returns the updated FEN with the move.
     * @return A new FEN with an updated move gets returned
     */
    @GetMapping("/make-move/")
    public ResponseEntity<String> makeMove(@RequestParam String FEN){
        System.out.println("===============================================================================================");
        System.out.println(FEN);
        game.getBoard().loadFen(FEN);
        int move = cfg.getBudgetMillis() > 0 ? game.getBot().findBestMove(game.getBoard().copy(), cfg.getDepth(), cfg.getBudgetMillis()) : game.getBot().findBestMove(game.getBoard().copy(), cfg.getDepth());

        if (move == 0) return ResponseEntity.ok(game.getBoard().getFen());

        // applyMove, not makeMove: it also advances the turn, records the move,
        // and re-runs checkmate/draw detection, so the returned FEN has the
        // right side to move.get
        GameRules.applyMove(game.getBoard(), move);

        return ResponseEntity.ok(game.getBoard().getFen());
    }
}