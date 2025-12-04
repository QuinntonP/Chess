package org.quinnton.chess.core;

import java.util.Arrays;
import java.util.HashMap;

public class Board {
    private final Masks masks;

    public Board(Masks masks) {
        this.masks = masks;
    }

    HashMap<Integer, MoveList> pseudoLegalMoves;

    protected long[] bitBoards = new long[Piece.values().length];
    private int turnCounter = 0;


    // castling
    boolean whiteKingHasMoved = false;
    boolean blackKingHasMoved = false;
    boolean whiteKingRookHasMoved = false;
    boolean whiteQueenRookHasMoved = false;
    boolean blackKingRookHasMoved = false;
    boolean blackQueenRookHasMoved = false;

    Move lastMove;
    Move lastWhiteMove;
    Move lastBlackMove;



    public void loadFen(String fen) {
        // Clear any previous position
        Arrays.fill(bitBoards, 0L);

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

        // initialize first moves
        pseudoLegalMoves = MoveGen.generatePseudoLegalMoves(this, masks, true);
    }


    public void makeMove(Move move){
        Piece piece = move.piece;
        Piece toPiece = move.capture;

        setBitboardBit(piece, move.from, false);
        setBitboardBit(piece, move.to, true);

        if (toPiece != null){
            setBitboardBit(toPiece, move.to, false);
        }

        // castling stuff
        if (move.flags == 2 || move.flags == 3){
            castleRooks(move);
        }

        checkCastlingPieces(move.from);
    }


    private void castleRooks(Move  move){
        Move rookMove = null;

        if (move.piece.isWhite()){
            // White Queen side rook
            if (move.flags == 2){
                rookMove = new Move(0, 3, Piece.WR, null, null, 0);
            }
            // White King side rook
            if (move.flags == 3){
                rookMove = new Move(7, 5, Piece.WR, null, null, 0);
            }
        }
        else{
            if (move.flags == 2){
                // Black Queen side rook
                rookMove = new Move(56, 59, Piece.BR, null, null, 0);
            }
            if (move.flags == 3){
                // Black King side rook
                rookMove = new Move(63, 61, Piece.BR, null, null, 0);
            }
        }

        if (rookMove != null){
            makeMove(rookMove);
        }
    }


    private void checkCastlingPieces(int square){
        switch (square){
            case 0 :
                whiteQueenRookHasMoved = true;
                break;
            case 7 :
                whiteKingRookHasMoved = true;
                break;
            case 56 :
                blackQueenRookHasMoved = true;
                break;
            case 63 :
                blackKingRookHasMoved = true;
                break;

            case 4 :
                whiteKingHasMoved = true;
                break;
            case 60 :
                blackKingHasMoved = true;
                break;
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


    /**
     *
     * @return True if it is whites turns and False if it is blacks turn
     */
    public boolean getTurnCounter() {
        if (turnCounter % 2 == 0){
            return true;
        }
        return false;
    }


    public void addTurnCounter() {
        this.turnCounter++;
        pseudoLegalMoves = MoveGen.generatePseudoLegalMoves(this, masks, true);
    }


    public HashMap<Integer, MoveList> getPseudoLegalMoves(){
        return this.pseudoLegalMoves;
    }


    public long getAttackMask(boolean byWhite) {
        long mask = 0L;

        // All pieces for the side whose attacks we want
        long bb = byWhite ? getAllWhitePieces() : getAllBlackPieces();

        HashMap<Integer, MoveList> allMoves = MoveGen.generatePseudoLegalMoves(this, masks, bb, false);

        // **** this is all move to's currently it needs to filter by if they are attacks for things like pawn pushes -> castling -> etc.
        for (MoveList moveList : allMoves.values()) {
            for (Move move : moveList) {
                int to = move.to;
                if (move.flags == 0){
                    mask |= 1L << to; // set the 'to' square as attacked
                }
            }
        }

        return mask;
    }

}
