package org.quinnton.chess.headless;

import org.quinnton.chess.core.Board;
import org.quinnton.chess.core.Masks;
import org.quinnton.chess.core.SoundsPlayer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**

 */
@SpringBootApplication
public final class HeadlessMain {

    public static void main(String[] args) {
        var ctx = SpringApplication.run(HeadlessMain.class, args);
        HeadlessConfig cfg = ctx.getBean(HeadlessConfig.class);
        SpringApplication.run(HeadlessMain.class, args);

        // No UI: keep the move path off the audio subsystem entirely.
        SoundsPlayer.setEnabled(false);

        Masks masks = new Masks();
        HeadlessGameRunner runner = new HeadlessGameRunner(cfg.getDepth(), cfg.getBudgetMillis(), cfg.getMaxPlies(), cfg.getLogMoves());

        Board board = new Board(masks);
        board.loadFen(cfg.getFEN());


    }
}
