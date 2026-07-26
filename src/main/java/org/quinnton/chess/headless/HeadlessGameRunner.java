package org.quinnton.chess.headless;

import org.quinnton.chess.bot.Bot;
import org.quinnton.chess.core.Board;
import org.quinnton.chess.core.GameRules;
import org.quinnton.chess.core.Move;

import java.util.ArrayList;
import java.util.List;

/**
 * Plays a full bot-vs-bot game with no UI and no threading.
 *
 * The search runs synchronously on the calling thread: a {@link Bot} keeps
 * mutable per-search state and is not safe to search two positions at once, so
 * the two players get separate {@link Bot} instances and move one at a time.
 * Nothing here touches JavaFX, so it runs in tests, CLIs, or a tournament loop.
 */
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

    /**
     * Play {@code board} to completion with {@code white} moving on white's turns
     * and {@code black} on black's. The board is mutated in place and left in its
     * final position.
     *
     * @return the game outcome
     */
    public GameResult play(Board board, Bot white, Bot black) {
        List<String> uciMoves = new ArrayList<>();
        long whiteNodes = 0;
        long blackNodes = 0;
        int plies = 0;
        boolean finishedNaturally = true;

        while (!board.gameOver) {
            if (plies >= maxPlies) {
                finishedNaturally = false;
                break;
            }

            boolean whiteToMove = board.getTurnCounter();
            Bot mover = whiteToMove ? white : black;

            // Search a copy so the live board stays pristine regardless of how the
            // bot manages its own make/unmake bookkeeping. A non-positive budget
            // means "no time limit": an exhaustive fixed-depth search, which is the
            // only mode that is bit-for-bit reproducible from the seed (a wall-clock
            // budget reaches different depths depending on machine load).
            int move = budgetMillis > 0
                    ? mover.findBestMove(board.copy(), maxDepth, budgetMillis)
                    : mover.findBestMove(board.copy(), maxDepth);

            if (whiteToMove) whiteNodes += mover.getNodes();
            else             blackNodes += mover.getNodes();

            if (move == 0) {
                // No move returned despite gameOver being false: treat as a
                // non-natural stop rather than inventing a result.
                finishedNaturally = false;
                break;
            }

            String uci = Move.toUci(move);
            uciMoves.add(uci);
            if (logMoves) {
                System.out.printf("%3d. %s %s%n",
                        plies + 1, whiteToMove ? "W" : "B", uci);
            }

            GameRules.applyMove(board, move);
            plies++;
        }

        return new GameResult(
                board.getWinnerIsWhite(),
                board.getEndReason(),
                plies,
                white.getSeed(),
                black.getSeed(),
                whiteNodes,
                blackNodes,
                uciMoves,
                finishedNaturally
        );
    }
}
