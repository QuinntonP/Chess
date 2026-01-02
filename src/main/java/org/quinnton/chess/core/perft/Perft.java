package org.quinnton.chess.core.perft;

import org.quinnton.chess.core.Board;
import org.quinnton.chess.core.Move;
import org.quinnton.chess.core.MoveGen;
import org.quinnton.chess.core.Masks;

public final class Perft {

    private static final int MAX_MOVES = 256;

    private Perft() {}

    /**
     * Standard recursive perft:
     * counts the number of leaf nodes at a given depth.
     */
    public static long perft(Board board, Masks masks, int depth) {
        if (depth == 0) return 1;

        int[] moves = new int[MAX_MOVES];
        int moveCount = MoveGen.generateLegalMovesFlat(board, masks, moves);

        long nodes = 0;
        for (int i = 0; i < moveCount; i++) {
            int m = moves[i];

            board.makeMoveInternal(m);
            nodes += perft(board, masks, depth - 1);
            board.unmakeMoveInternal(m);
        }

        return nodes;
    }

    /**
     * Root perft: prints each root move and its subtree count.
     * Good for comparing to perft tables (like perftree output).
     */
    public static long perftRoot(Board board, Masks masks, int depth) {
        int[] moves = new int[MAX_MOVES];
        int moveCount = MoveGen.generateLegalMovesFlat(board, masks, moves);

        long total = 0;
        long totalCastling = 0;

        for (int i = 0; i < moveCount; i++) {
            int m = moves[i];

            board.makeMoveInternal(m);
            long count = perft(board, masks, depth - 1);
            board.unmakeMoveInternal(m);

            System.out.printf("%s: %d%n", Move.toUci(m), count);
            total += count;

            int f = Move.flags(m);
            if (f == Move.FLAG_CASTLE_QS || f == Move.FLAG_CASTLE_KS) {
                totalCastling += count;
            }
        }

        System.out.println("Total castling is: " + totalCastling);
        System.out.printf("Total nodes at depth %d: %d%n", depth, total);
        return total;
    }
}
