package org.quinnton.chess.core;

public class Board {
    private boolean whiteToMove = true;
    protected long[] bitBoards = new long[Piece.values().length];

    Move lastMove;
    Move lastWhiteMove;
    Move lastBlackMove;

    public void loadFen(String fen) {
        // Clear any previous position
        for (int i = 0; i < bitBoards.length; i++) bitBoards[i] = 0L;

        String placement = fen.trim().split("\\s+")[0];

        int rank = 7; // top row (a8)
        int file = 0; // a-file

        for (int i = 0; i < placement.length(); i++) {
            char c = placement.charAt(i);

            if (c == '/') {
                rank--;
                file = 0;
                continue;
            }

            if (Character.isDigit(c)) {
                file += (c - '0'); // skip that many empty squares
                continue;
            }

            Piece piece = switch (c) {
                case 'P' -> Piece.WP; case 'R' -> Piece.WR; case 'N' -> Piece.WN;
                case 'B' -> Piece.WB; case 'Q' -> Piece.WQ; case 'K' -> Piece.WK;
                case 'p' -> Piece.BP; case 'r' -> Piece.BR; case 'n' -> Piece.BN;
                case 'b' -> Piece.BB; case 'q' -> Piece.BQ; case 'k' -> Piece.BK;
                default  -> throw new IllegalArgumentException("Bad piece char: " + c);
            };

            int idx = rank * 8 + file; // a1=0 … a8=56 … h8=63
            setBitboardBit(piece, idx, true);
            file++;
        }
    }


    public void makeMove(int from, int to){
        Piece piece = getPieceAtSquare(from);
        Piece toPiece = getPieceAtSquare(to);

        setBitboardBit(piece, from, false);
        setBitboardBit(piece, to, true);

        if (toPiece != null){
            setBitboardBit(toPiece, to, false);
        }
    }


    public void setLastMove(Move move){
        lastMove = move;

        if (move.piece.isWhite()){
            lastWhiteMove = move;
        }
        else{
            lastBlackMove = move;
        }

        playMoveSound(move);
    }

    private void playMoveSound(Move move){
        if (move.promo != null){
            SoundsPlayer.playPromoteSound();
        } else if (move.capture != null) {
            SoundsPlayer.playCaptureSound();
            // temp for if a castle flag was 2
        } else if (move.flags == 2) {
            SoundsPlayer.playCastleSound();
        } else if (move.flags == 1) {
            // temp for if a check flag was 1
            SoundsPlayer.playMoveCheckSound();
        } else {
            SoundsPlayer.playMoveSelfSound();
        }
    }

    public Move getLastMove(){
        return lastMove;
    }

    public Move getLastWhiteMove(){
        return lastWhiteMove;
    }

    public Move getLastBlackMove(){
        return lastBlackMove;
    }

    public void setBitboardBit(Piece piece, int square, boolean set) {
        long mask = 1L << square;
        if (set) {
            bitBoards[piece.ordinal()] |= mask;
        } else {
            bitBoards[piece.ordinal()] &= ~mask;
        }
    }


    public Piece getPieceAtSquare(int square) {
        long mask = 1L << square;

        for (int i = 0; i < bitBoards.length; i++) {
            if ((bitBoards[i] & mask) != 0) {
                return Piece.values()[i]; // map index -> Piece enum
            }
        }

        return null; // empty square
    }


    public long getAllWhitePieces() {
        long mask = 0L;
        Piece[] whites = { Piece.WK, Piece.WQ, Piece.WB, Piece.WR, Piece.WN, Piece.WP };
        for (Piece p : whites) {
            mask |= bitBoards[p.ordinal()];
        }
        return mask;
    }


    public long getAllBlackPieces() {
        long mask = 0L;
        Piece[] blacks = { Piece.BK, Piece.BQ, Piece.BB, Piece.BR, Piece.BN, Piece.BP };
        for (Piece p : blacks) mask |= bitBoards[p.ordinal()];
        return mask;
    }


    public long getAllPieces() {
        return getAllWhitePieces() | getAllBlackPieces();
    }


    public long getAllPawns() {
        return bitBoards[Piece.WP.ordinal()] | bitBoards[Piece.BP.ordinal()];
    }
}
