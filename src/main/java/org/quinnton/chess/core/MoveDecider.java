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

    /** Extra time beyond the search budget before the watchdog forces the search to stop. */
    private static final long WATCHDOG_GRACE_MILLIS = 500;

    private final Board board;
    private final Bot bot;
    private final boolean humanIsWhite;
    private final int searchDepth;
    private final long budgetMillis;

    /** Invoked on the JavaFX thread after a bot move is applied (e.g. to clear UI selection). */
    private Runnable onBotMoveApplied;

    public MoveDecider(Board board, Bot bot, boolean humanIsWhite, int searchDepth, long budgetMillis) {
        this.board = board;
        this.bot = bot;
        this.humanIsWhite = humanIsWhite;
        this.searchDepth = searchDepth;
        this.budgetMillis = budgetMillis;
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
     * Kick off play. If the bot owns the opening move (i.e. the human is Black), this
     * triggers the bot's first move; otherwise it waits for the human. Safe to call once
     * after the game is wired up.
     */
    public void start() {
        maybeGenBotMove();
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
        GameRules.applyMove(board, move);
    }

    private void maybeGenBotMove() {
        if (board.gameOver) return;
        if (isHumanTurn()) return; // nothing for the bot to do

        Thread searchThread = new Thread(() -> {
            Board searchBoard = board.copy();
            int best = bot.findBestMove(searchBoard, searchDepth, budgetMillis); // encoded int
            if (best == 0) return;

            javafx.application.Platform.runLater(() -> {
                applyMove(best);
                if (onBotMoveApplied != null) onBotMoveApplied.run();
            });
        }, "Bot-Search-Thread");
        searchThread.start();

        // Watchdog: the search aborts itself at its time budget via an internal clock
        // check, but a pathological hang (e.g. a loop that never re-enters that check)
        // would never return. After the budget plus a grace period we ask it to stop;
        // if it still has not finished we log it and apply no move rather than trying to
        // kill the thread, which Java cannot do safely.
        Thread watchdog = new Thread(() -> {
            try {
                searchThread.join(budgetMillis + WATCHDOG_GRACE_MILLIS);
                if (searchThread.isAlive()) {
                    bot.requestStop();
                    searchThread.join(WATCHDOG_GRACE_MILLIS);
                    if (searchThread.isAlive()) {
                        System.err.println("Bot search hung past its budget; no move applied this turn.");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Bot-Search-Watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }
}
