package org.quinnton.chess.headless;

import org.quinnton.chess.core.Board;
import org.quinnton.chess.core.SoundsPlayer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Boots the headless server. The game itself lives in {@link GameService},
 * which the REST layer injects.
 */
@SpringBootApplication
public final class HeadlessMain {

    public static void main(String[] args) {
        // No UI: keep the move path off the audio subsystem entirely.
        // Set before the context starts, since GameService builds a board during startup.
        SoundsPlayer.setEnabled(false);
        var ctx = SpringApplication.run(HeadlessMain.class, args);

        GameService game = ctx.getBean(GameService.class);   // same object RestAPI has
        Board board = game.getBoard();
    }
}
