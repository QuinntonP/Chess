package org.quinnton.chess.core;

public final class MoveBuffer {
    public final Move[] moves;
    public int size;

    public MoveBuffer(int cap) {
        this.moves = new Move[cap];
    }

    public void clear() { size = 0; }

    public void add(Move m) { moves[size++] = m; }

    public boolean isEmpty() {
        return size == 0;
    }
}
