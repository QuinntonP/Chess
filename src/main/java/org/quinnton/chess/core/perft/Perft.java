package org.quinnton.chess.core.perft;

import org.quinnton.chess.core.*;

import java.util.HashMap;

public final class Perft {

    private Perft() {}

    /**
     * Standard recursive perft:
     * counts the number of leaf nodes at a given depth.
     */
    public static long perft(Board board, Masks masks, int depth) {
        if (depth == 0) {
            return 1;
        }

        HashMap<Integer, MoveList> moves = MoveGen.generateLegalMoves(board, masks);
        long nodes = 0;

        for (MoveList list : moves.values()) {
            for (Move move : list) {
                board.makeMoveInternal(move);
                nodes += perft(board, masks, depth - 1);
                board.unmakeMoveInternal(move);
            }
        }

        return nodes;
    }

    /**
     * Root perft: prints each root move and its subtree count.
     * Good for comparing to perft tables (like perftree output).
     */
    public static long perftRoot(Board board, Masks masks, int depth) {
        HashMap<Integer, MoveList> moves = MoveGen.generateLegalMoves(board, masks);
        long total = 0;
        long totalCastling = 0;

        for (var entry : moves.entrySet()) {
            int fromSq = entry.getKey();
            MoveList list = entry.getValue();

            for (Move move : list) {
                board.makeMoveInternal(move);
                long count = perft(board, masks, depth - 1);
                board.unmakeMoveInternal(move);

                // Adjust Move#toString() however you like (SAN/uci/algebraic)
                System.out.printf("%s: %d%n", moveToString(move), count);
                total += count;

                if (move.flags == 2 || move.flags == 3) {
                    totalCastling += count;
                }

            }
        }

        System.out.println("Total castling is: " + totalCastling);
        System.out.printf("Total nodes at depth %d: %d%n", depth, total);
        return total;
    }

    // Minimal algebraic-ish formatter (file/rank only, no disambiguation)
    private static String moveToString(Move m) {
        return squareToString(m.from) + squareToString(m.to);
    }

    private static String squareToString(int sq) {
        int file = sq % 8;
        int rank = sq / 8;
        char f = (char) ('a' + file);
        char r = (char) ('1' + rank);
        return "" + f + r;
    }
}
