package org.quinnton.chess.core;

    public enum Piece {
        WP(true, 'P', "white-pawn"), WN(true, 'N', "white-knight"), WB(true, 'B', "white-bishop"),
        WR(true, 'R', "white-rook"), WQ(true, 'Q', "white-queen"), WK(true, 'K', "white-king"),
        BP(false, 'p', "black-pawn"), BN(false, 'n', "black-knight"), BB(false, 'b', "black-bishop"),
        BR(false, 'r', "black-rook"), BQ(false, 'q', "black-queen"), BK(false, 'k', "black-king");

        public final boolean white;
        public final char symbol;
        public final String name;

        Piece(boolean white, char symbol, String name) {
            this.white = white;
            this.symbol = symbol;
            this.name = name;
        }

        public boolean isWhite() { return white; }
        public boolean isBlack() { return !white; }
    }