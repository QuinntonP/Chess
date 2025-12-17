package org.quinnton.chess.bot;

import org.quinnton.chess.core.*;

public class Bot {

    private static final int MATE = 1_000_000;

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

        MoveBuffer moves = MoveGen.generateLegalMovesFlat(board, board.masks);

        if (moves.isEmpty()) {
            boolean whiteToMove = board.getTurnCounter();
            boolean inCheck = whiteToMove ? board.whiteInCheck : board.blackInCheck;

            if (inCheck) {
                return whiteToMove ? (-MATE + ply) : (MATE - ply);
            }
            return 0;
        }

        if (depth == 0) {
            return board.evaluate.score();
        }

        boolean maximizing = board.getTurnCounter();

        if (maximizing) {
            int best = Integer.MIN_VALUE;

            for (int i = 0; i < moves.size; i++) {
                Move move = moves.moves[i];

                Evaluate.EvalUndo undo = board.evaluate.updateMakeMove(move);
                board.makeMoveInternal(move);

                int val = alphaBeta(board, depth - 1, ply + 1, alpha, beta);

                board.unmakeMoveInternal(move);
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

            for (int i = 0; i < moves.size; i++) {
                Move move = moves.moves[i];

                Evaluate.EvalUndo undo = board.evaluate.updateMakeMove(move);
                board.makeMoveInternal(move);

                int val = alphaBeta(board, depth - 1, ply + 1, alpha, beta);

                board.unmakeMoveInternal(move);
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

    public Move findBestMove(Board board, int depth) {
        resetStats();

        MoveBuffer moves = MoveGen.generateLegalMovesFlat(board, board.masks);

        boolean maximizing = board.getTurnCounter();
        Move bestMove = null;
        int bestScore = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        int alpha = Integer.MIN_VALUE + 1;
        int beta  = Integer.MAX_VALUE;

        for (int i = 0; i < moves.size; i++) {
            Move move = moves.moves[i];

            Evaluate.EvalUndo undo = board.evaluate.updateMakeMove(move);
            board.makeMoveInternal(move);

            int score = alphaBeta(board, depth - 1, 1, alpha, beta);

            board.unmakeMoveInternal(move);
            board.evaluate.updateUnmakeMove(undo);

            if (maximizing) {
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
                System.out.println("The quick brown fox jumps over the lazy dog. \n yo");
                alpha = Math.max(alpha, bestScore);
            } else {
                if (score < bestScore) {
                    bestScore = score;
                    bestMove = move;
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
                bestMove == null ? "null" : bestMove.toString(),
                bestScore
        );

        return bestMove;
    }
}
