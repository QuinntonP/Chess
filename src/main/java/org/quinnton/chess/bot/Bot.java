package org.quinnton.chess.bot;

import org.quinnton.chess.core.Board;
import org.quinnton.chess.core.Move;
import org.quinnton.chess.core.MoveGen;

import java.util.Random;

public class Bot {

    private static final int MATE = 1_000_000;
    private static final int MAX_MOVES = 256;

    /** How often (in nodes) the search polls the wall clock / stop flag. Power of two. */
    private static final int CLOCK_CHECK_MASK = 2047;

    /** Thrown to unwind the recursion when the time budget is exhausted or a stop is requested. */
    private static final class SearchAborted extends RuntimeException {
        private SearchAborted() {
            super(null, null, false, false); // no message, no stack trace — this is control flow, not an error
        }
    }
    private static final SearchAborted ABORTED = new SearchAborted();

    private long nodes;

    // Time-budget state. NOTE: a single Bot instance shares this state, so it must not
    // be used to search two positions concurrently. That is fine here: one bot replies
    // to one position at a time.
    private volatile boolean stop;
    private long deadlineNanos;

    // Seeded RNG used only to break ties between equally-best moves, so repeated
    // games are not byte-for-byte identical. The default seed varies per instance
    // (different games diverge); pass an explicit seed for reproducible play.
    private final long seed;
    private final Random rng;

    public Bot() {
        this(System.nanoTime());
    }

    public Bot(long seed) {
        this.seed = seed;
        this.rng = new Random(seed);
    }

    public long getSeed() {
        return seed;
    }

    private void resetStats() {
        nodes = 0;
    }

    public long getNodes() {
        return nodes;
    }

    /** Asks an in-flight search on another thread to abort at its next clock check. */
    public void requestStop() {
        stop = true;
    }

    public int alphaBeta(Board board, int depth, int ply, int alpha, int beta) {
        nodes++;

        if ((nodes & CLOCK_CHECK_MASK) == 0 && (stop || System.nanoTime() >= deadlineNanos)) {
            throw ABORTED;
        }

        int[] moves = new int[MAX_MOVES];
        int moveCount = MoveGen.generateLegalMovesFlat(board, board.masks, moves);

        if (moveCount == 0) {
            boolean whiteToMove = board.getTurnCounter();
            boolean inCheck = whiteToMove ? board.whiteInCheck : board.blackInCheck;

            if (inCheck) {
                return whiteToMove ? (-MATE + ply) : (MATE - ply);
            }
            return 0; // stalemate
        }

        if (depth == 0) {
            return board.evaluate.score();
        }

        boolean maximizing = board.getTurnCounter();

        if (maximizing) {
            int best = Integer.MIN_VALUE;

            for (int i = 0; i < moveCount; i++) {
                int m = moves[i];

                Evaluate.EvalUndo undo = board.evaluate.updateMakeMove(m);
                board.makeMoveInternal(m);

                int val = alphaBeta(board, depth - 1, ply + 1, alpha, beta);

                board.unmakeMoveInternal(m);
                board.evaluate.updateUnmakeMove(undo);

                if (val > best) best = val;
                if (best > alpha) alpha = best;

                if (alpha >= beta) {
                    return best;
                }
            }
            return best;
        } else {
            int best = Integer.MAX_VALUE;

            for (int i = 0; i < moveCount; i++) {
                int m = moves[i];

                Evaluate.EvalUndo undo = board.evaluate.updateMakeMove(m);
                board.makeMoveInternal(m);

                int val = alphaBeta(board, depth - 1, ply + 1, alpha, beta);

                board.unmakeMoveInternal(m);
                board.evaluate.updateUnmakeMove(undo);

                if (val < best) best = val;
                if (best < beta) beta = best;

                if (alpha >= beta) {
                    return best;
                }
            }
            return best;
        }
    }

    public int search(Board board, int depth) {
        resetStats();
        stop = false;
        deadlineNanos = Long.MAX_VALUE;

        int score = alphaBeta(
                board,
                depth,
                0,
                Integer.MIN_VALUE + 1,
                Integer.MAX_VALUE
        );

        return score;
    }

    /**
     * Fixed-depth search with no time limit. Kept for callers (and tests) that want
     * an exhaustive search to a known depth.
     */
    public int findBestMove(Board board, int depth) {
        resetStats();
        stop = false;
        deadlineNanos = Long.MAX_VALUE;
        return searchRoot(board, depth);
    }

    /**
     * Iterative-deepening search bounded by a wall-clock budget. Searches depth 1, 2,
     * 3, ... up to {@code maxDepth}, keeping the best move from the last depth that
     * completed in time. If the budget runs out partway through a depth, that depth's
     * partial work is discarded and the previous depth's move is played, so the
     * returned move is always fully evaluated.
     *
     * @param maxDepth    hard cap on search depth (a safety ceiling; the budget is the
     *                    usual limiter)
     * @param budgetMillis approximate wall-clock budget for the whole search
     * @return the chosen move (encoded int), or 0 if there are no legal moves
     */
    public int findBestMove(Board board, int maxDepth, long budgetMillis) {
        resetStats();
        stop = false;
        deadlineNanos = System.nanoTime() + budgetMillis * 1_000_000L;

        int best = 0;
        for (int depth = 1; depth <= maxDepth; depth++) {
            try {
                int move = searchRoot(board, depth);
                best = move;            // this depth finished in time; trust its result
                if (move == 0) break;   // no legal moves — deeper search won't change that
            } catch (SearchAborted aborted) {
                break;                  // ran out of time mid-depth; keep the last completed depth
            }
        }
        return best;
    }

    private int searchRoot(Board board, int depth) {
        int[] moves = new int[MAX_MOVES];
        int moveCount = MoveGen.generateLegalMovesFlat(board, board.masks, moves);

        boolean maximizing = board.getTurnCounter();
        int bestScore = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        // every move that is provably tied for the best score
        int[] bestMoves = new int[moveCount];
        int bestCount = 0;

        int alpha = Integer.MIN_VALUE + 1;
        int beta  = Integer.MAX_VALUE;

        for (int i = 0; i < moveCount; i++) {
            int m = moves[i];
            int score = scoreMove(board, m, depth, alpha, beta);

            if (maximizing) {
                if (score > bestScore) {
                    // strictly better, and (alpha < score < beta) so the value is exact
                    bestScore = score;
                    bestMoves[0] = m;
                    bestCount = 1;
                    alpha = Math.max(alpha, bestScore);
                } else if (score == bestScore && isExactTie(board, m, depth, bestScore)) {
                    bestMoves[bestCount++] = m;
                }
            } else {
                if (score < bestScore) {
                    bestScore = score;
                    bestMoves[0] = m;
                    bestCount = 1;
                    beta = Math.min(beta, bestScore);
                } else if (score == bestScore && isExactTie(board, m, depth, bestScore)) {
                    bestMoves[bestCount++] = m;
                }
            }
        }

        if (bestCount == 0) return 0; // 0 = none (no legal moves)
        if (bestCount == 1) return bestMoves[0];
        return bestMoves[rng.nextInt(bestCount)];
    }

    // Apply m, search the resulting position to `depth - 1`, then unmake it.
    private int scoreMove(Board board, int m, int depth, int alpha, int beta) {
        Evaluate.EvalUndo undo = board.evaluate.updateMakeMove(m);
        board.makeMoveInternal(m);

        int score = alphaBeta(board, depth - 1, 1, alpha, beta);

        board.unmakeMoveInternal(m);
        board.evaluate.updateUnmakeMove(undo);
        return score;
    }

    /**
     * A move that merely *returns* the current best score under a narrowed window
     * may only be a fail-low/high bound, not its true value. Re-search it with a
     * full window so we never add an inferior move to the tie pool.
     */
    private boolean isExactTie(Board board, int m, int depth, int bestScore) {
        int exact = scoreMove(board, m, depth, Integer.MIN_VALUE + 1, Integer.MAX_VALUE);
        return exact == bestScore;
    }
}
