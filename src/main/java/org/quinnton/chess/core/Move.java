package org.quinnton.chess.core;

public final class Move {
    public final int from, to;
    public final Piece piece, capture, promo;
    public final int flags; // bits for EP, castle, double push, etc.

    public Move(int from, int to, Piece p, Piece cap, Piece promo, int flags){
        this.from=from; this.to=to; this.piece=p; this.capture=cap; this.promo=promo; this.flags=flags;
    }
}
