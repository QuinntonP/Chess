package org.quinnton.chess.core.perft;

public class PerftPosition {
    public final String name;      // e.g. "startpos"
    public final String fen;
    public final long[] expected;  // expected[depth] = nodes at that depth

    /**
     * expected[0] can be 1 (root), expected[1] is depth 1, etc.
     * For example, for start position: new long[]{1, 20, 400, 8902, ...}
     */
    public PerftPosition(String name, String fen, long[] expected) {
        this.name = name;
        this.fen = fen;
        this.expected = expected;
    }
}
