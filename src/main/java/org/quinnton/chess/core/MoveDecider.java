package org.quinnton.chess.core;

import org.quinnton.chess.bot.Bot;

/**
 * Owns turn ownership and move application for a human-vs-bot game.
 *
 * SelectionController only translates clicks into a chosen move for the human's
 * color and hands it here via {@link #submitHumanMove(int)}. This class applies
 * the move, advances the turn, checks for mate, and then asks the bot to reply.
 */
public class MoveDecider {

    private final Board board;
    private final Bot bot;
    private final boolean humanIsWhite;
    private final int searchDepth;

    /** Invoked on the JavaFX thread after a bot move is applied (e.g. to clear UI selection). */
    private Runnable onBotMoveApplied;

    public MoveDecider(Board board, Bot bot, boolean humanIsWhite, int searchDepth) {
        this.board = board;
        this.bot = bot;
        this.humanIsWhite = humanIsWhite;
        this.searchDepth = searchDepth;
    }

    public boolean isHumanWhite() {
        return humanIsWhite;
    }

    public boolean isHumanTurn() {
        return board.getTurnCounter() == humanIsWhite;
    }

    public void setOnBotMoveApplied(Runnable onBotMoveApplied) {
        this.onBotMoveApplied = onBotMoveApplied;
    }

    /**
     * Apply the human's chosen move, then let the bot respond if it is now its turn.
     * No-ops if the game is over, the move is invalid, or it is not the human's turn.
     */
    public void submitHumanMove(int move) {
        if (board.gameOver || move == 0) return;
        if (!isHumanTurn()) return;

        applyMove(move);
        maybeGenBotMove();
    }

    private void applyMove(int move) {
        board.makeMove(move);
        board.addTurnCounter();
        board.setLastMove(move);
        board.lookForCheckmate();
        board.lookForDraw(move);
    }

    private void maybeGenBotMove() {
        if (board.gameOver) return;
        if (isHumanTurn()) return; // nothing for the bot to do

        new Thread(() -> {
            Board searchBoard = board.copy();
            int best = bot.findBestMove(searchBoard, searchDepth); // encoded int
            if (best == 0) return;

            javafx.application.Platform.runLater(() -> {
                applyMove(best);
                if (onBotMoveApplied != null) onBotMoveApplied.run();
            });
        }, "Bot-Search-Thread").start();
    }
}
