package org.quinnton.chess.core.perft;

import org.quinnton.chess.core.Board;
import org.quinnton.chess.core.Masks;

import java.util.List;

public final class PerftRunner {

    private PerftRunner() {}

    public static void runPerftSuite(List<PerftPosition> tests, Masks masks) {
        Board board = new Board(masks);

        for (PerftPosition pos : tests) {
            System.out.println("=== " + pos.name + " ===");
            board.loadFen(pos.fen);

            int maxDepth = pos.expected.length - 1; // assuming index 0 is depth 0

            for (int depth = 1; depth <= maxDepth; depth++) {
                long got = Perft.perft(board, masks, depth);
                long expected = pos.expected[depth];

                String status = (got == expected) ? "OK" : "MISMATCH";
                System.out.printf(
                        "Depth %d: got=%d, expected=%d -> %s%n",
                        depth, got, expected, status
                );
            }

            System.out.println();
        }
    }
}
