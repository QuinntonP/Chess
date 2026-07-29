package org.quinnton.chess.headless;

import org.quinnton.chess.bot.Bot;
import org.quinnton.chess.core.Board;
import org.quinnton.chess.core.GameRules;
import org.quinnton.chess.core.Move;

import java.util.ArrayList;
import java.util.List;

public final class HeadlessGameRunner {

    /** Safety ceiling so a pathological game can never loop forever. */
    public static final int DEFAULT_MAX_PLIES = 600;

    private final int maxDepth;
    private final long budgetMillis;
    private final int maxPlies;
    private final boolean logMoves;

    public HeadlessGameRunner(int maxDepth, long budgetMillis) {
        this(maxDepth, budgetMillis, DEFAULT_MAX_PLIES, false);
    }

    public HeadlessGameRunner(int maxDepth, long budgetMillis, int maxPlies, boolean logMoves) {
        this.maxDepth = maxDepth;
        this.budgetMillis = budgetMillis;
        this.maxPlies = maxPlies;
        this.logMoves = logMoves;
    }


    public int getMove(Board board, Bot bot){
        int move = budgetMillis > 0 ? bot.findBestMove(board.copy(), maxDepth, budgetMillis) : bot.findBestMove(board.copy(), maxDepth);
        return move;
    }

}
