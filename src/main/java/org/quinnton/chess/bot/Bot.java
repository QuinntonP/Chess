package org.quinnton.chess.bot;

import org.quinnton.chess.core.Board;
import org.quinnton.chess.core.Move;
import org.quinnton.chess.core.MoveGen;
import org.quinnton.chess.core.MoveList;

public class Bot {

    private static final int MATE = 1_000_000;

    public int alphaBeta(Board board, int depth, int ply, int alpha, int beta) {

        // Generate legal moves for THIS position
        var moves = MoveGen.generateLegalMoves(board, board.masks);

        if (moves.isEmpty()) {
            boolean whiteToMove = board.getTurnCounter();
            boolean inCheck = whiteToMove ? board.whiteInCheck : board.blackInCheck;

            if (inCheck) {
                // side to move is checkmated
                return whiteToMove ? (-MATE + ply) : (MATE - ply);
            }
            return 0; // stalemate
        }

        // Leaf
        if (depth == 0) {
            return board.evaluate.score();
        }

        boolean maximizing = board.getTurnCounter();

        if (maximizing) {
            int best = Integer.MIN_VALUE;

            for (MoveList list : moves.values()) {
                for (Move move : list) {

                    // MAKE
                    Evaluate.EvalUndo undo = board.evaluate.updateMakeMove(move);
                    board.makeMoveInternal(move);

                    int val = alphaBeta(board, depth - 1, ply + 1, alpha, beta);

                    // UNMAKE
                    board.unmakeMoveInternal(move);
                    board.evaluate.updateUnmakeMove(undo);

                    if (val > best) best = val;
                    if (best > alpha) alpha = best;

                    // Beta cutoff
                    if (alpha >= beta) {
                        return best;
                    }
                }
            }
            return best;

        } else {
            int best = Integer.MAX_VALUE;

            for (MoveList list : moves.values()) {
                for (Move move : list) {

                    // MAKE
                    Evaluate.EvalUndo undo = board.evaluate.updateMakeMove(move);
                    board.makeMoveInternal(move);

                    int val = alphaBeta(board, depth - 1, ply + 1, alpha, beta);

                    // UNMAKE
                    board.unmakeMoveInternal(move);
                    board.evaluate.updateUnmakeMove(undo);

                    if (val < best) best = val;
                    if (best < beta) beta = best;

                    // Alpha cutoff
                    if (alpha >= beta) {
                        return best;
                    }
                }
            }
            return best;
        }
    }


    public int search(Board board, int depth) {
        return alphaBeta(board, depth, 0, Integer.MIN_VALUE + 1, Integer.MAX_VALUE);
    }


    public Move findBestMove(Board board, int depth) {
        var moves = MoveGen.generateLegalMoves(board, board.masks);

        boolean maximizing = board.getTurnCounter(); // true=white
        Move bestMove = null;
        int bestScore = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        int alpha = Integer.MIN_VALUE + 1;
        int beta  = Integer.MAX_VALUE;

        for (MoveList list : moves.values()) {
            for (Move move : list) {

                // MAKE
                Evaluate.EvalUndo undo = board.evaluate.updateMakeMove(move);
                board.makeMoveInternal(move);

                // score after this move (one ply deeper)
                int score = alphaBeta(board, depth - 1, 1, alpha, beta);

                // UNMAKE
                board.unmakeMoveInternal(move);
                board.evaluate.updateUnmakeMove(undo);

                if (maximizing) {
                    if (score > bestScore) {
                        bestScore = score;
                        bestMove = move;
                    }
                    alpha = Math.max(alpha, bestScore); // root alpha update (optional)
                } else {
                    if (score < bestScore) {
                        bestScore = score;
                        bestMove = move;
                    }
                    beta = Math.min(beta, bestScore);   // root beta update (optional)
                }
            }
        }

        return bestMove;
    }

}
