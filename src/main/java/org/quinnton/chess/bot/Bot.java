package org.quinnton.chess.bot;

import org.quinnton.chess.core.Board;
import org.quinnton.chess.core.Move;
import org.quinnton.chess.core.MoveGen;

public class Bot {

    private static final int MATE = 1_000_000;
    private static final int MAX_MOVES = 256;

    private long nodes;
    private long startNanos;

    private void resetStats() {
        nodes = 0;
        startNanos = System.nanoTime();
    }

    private long elapsedMillis() {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    public long getNodes() {
        return nodes;
    }

    public int alphaBeta(Board board, int depth, int ply, int alpha, int beta) {
        nodes++;

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

        int score = alphaBeta(
                board,
                depth,
                0,
                Integer.MIN_VALUE + 1,
                Integer.MAX_VALUE
        );

        long ms = elapsedMillis();
        double nps = ms > 0 ? (nodes * 1000.0) / ms : nodes;

        System.out.printf(
                "Bot search depth=%d time=%dms nodes=%d nps=%.0f score=%d%n",
                depth, ms, nodes, nps, score
        );

        return score;
    }

    public int findBestMove(Board board, int depth) {
        resetStats();

        int[] moves = new int[MAX_MOVES];
        int moveCount = MoveGen.generateLegalMovesFlat(board, board.masks, moves);

        boolean maximizing = board.getTurnCounter();
        int bestMove = 0; // 0 = none
        int bestScore = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        int alpha = Integer.MIN_VALUE + 1;
        int beta  = Integer.MAX_VALUE;

        for (int i = 0; i < moveCount; i++) {
            int m = moves[i];

            Evaluate.EvalUndo undo = board.evaluate.updateMakeMove(m);
            board.makeMoveInternal(m);

            int score = alphaBeta(board, depth - 1, 1, alpha, beta);

            board.unmakeMoveInternal(m);
            board.evaluate.updateUnmakeMove(undo);

            if (maximizing) {
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = m;
                }
                alpha = Math.max(alpha, bestScore);
            } else {
                if (score < bestScore) {
                    bestScore = score;
                    bestMove = m;
                }
                beta = Math.min(beta, bestScore);
            }
        }

        long ms = elapsedMillis();
        double nps = ms > 0 ? (nodes * 1000.0) / ms : nodes;

        System.out.printf(
                "Bot findBestMove depth=%d time=%dms nodes=%d nps=%.0f best=%s score=%d%n",
                depth,
                ms,
                nodes,
                nps,
                bestMove == 0 ? "null" : Move.toUci(bestMove),
                bestScore
        );

        return bestMove;
    }
}
