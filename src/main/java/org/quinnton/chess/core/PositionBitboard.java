package org.quinnton.chess.core;

import java.util.Objects;

public class PositionBitboard {
    private final int position;
    private final long bitboard;

    public PositionBitboard(int position, long bitboard) {
        this.position = position;
        this.bitboard = bitboard;
    }

    public int getPosition() {
        return position;
    }

    public long getBitboard() {
        return bitboard;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PositionBitboard other)) return false;
        return position == other.position && bitboard == other.bitboard;
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, bitboard);
    }
}
