package org.quinnton.chess.headless;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestAPI {
    public HeadlessConfig cfg;

    public RestAPI(HeadlessConfig cfg) {
        this.cfg = cfg;
    }

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, World!\n";
    }

    @GetMapping("/set-depth/{depth}")
    public ResponseEntity<String> setDepth(@PathVariable int depth) {
        cfg.setDepth(depth);
        return ResponseEntity.ok("Color Is White: " + depth);
    }

    @GetMapping("/set-budgetmillis/{budgetmillis}")
    public ResponseEntity<String> setBudgetMillis(@PathVariable long budgetMilis) {
        cfg.setBudgetMillis(budgetMilis);
        return ResponseEntity.ok("Color Is White: " + budgetMilis);
    }

    @GetMapping("/set-maxPlies/{maxPlies}")
    public ResponseEntity<String> setMaxPlies(@PathVariable int maxPlies) {
        cfg.setMaxPlies(maxPlies);
        return ResponseEntity.ok("Color Is White: " + maxPlies);
    }

    @GetMapping("/set-color/{isWhite}")
    public ResponseEntity<String> setColor(@PathVariable boolean isWhite) {
        cfg.setIsWhite(isWhite);
        return ResponseEntity.ok("Color Is White: " + isWhite);
    }

    @GetMapping("/set-logMovesr/{logMoves}")
    public ResponseEntity<String> setLogMoves(@PathVariable boolean logMoves) {
        cfg.setLogMoves(logMoves);
        return ResponseEntity.ok("Logging moves is: " + logMoves);
    }

    @GetMapping("/set-setFEN/{setFEN}")
    public ResponseEntity<String> setFEN(@PathVariable String FEN) {
        cfg.setFEN(FEN);
        return ResponseEntity.ok("Logging moves is: " + FEN);
    }
}