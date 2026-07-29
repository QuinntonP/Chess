package org.quinnton.chess.headless;

import org.quinnton.chess.bot.Bot;
import org.quinnton.chess.core.Board;
import org.quinnton.chess.core.Masks;
import org.springframework.stereotype.Service;

/**
 * Owns the live headless game so anything in the Spring context can reach it.
 *
 * The board used to be a local in {@code HeadlessMain}, built after the context
 * had already started, so nothing else could see it.
 */
@Service
public class GameService {

    private final HeadlessConfig cfg;
    private final Masks masks = new Masks();
    private Board board;
    private Bot bot;

    public GameService(HeadlessConfig cfg) {
        this.cfg = cfg;
        newGame();
    }

    /** Discard the current game and start over from the FEN currently in config. */
    public void newGame() {
        board = new Board(masks);
        board.loadFen(cfg.getFEN());
        bot = new Bot();
    }

    public Board getBoard() {
        return board;
    }

    public Bot getBot() { return bot; }
}
