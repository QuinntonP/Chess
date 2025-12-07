package org.quinnton.chess.core;

import java.util.Arrays;
import java.util.HashMap;

public class Board {
    private final Masks masks;

    public Board(Masks masks) {
        this.masks = masks;
    }

    HashMap<Integer, MoveList> legalMoves;

    protected long[] bitBoards = new long[Piece.values().length];
    private int turnCounter = 0;


    // castling
    boolean whiteKingHasMoved = false;
    boolean blackKingHasMoved = false;
    boolean whiteKingRookHasMoved = false;
    boolean whiteQueenRookHasMoved = false;
    boolean blackKingRookHasMoved = false;
    boolean blackQueenRookHasMoved = false;


    // checks
    boolean whiteInCheck;
    boolean blackInCheck;

    // --- game state flags ---
    boolean gameOver = false;
    boolean stalemate = false;
    Boolean winnerIsWhite = null; // null = no winner yet / draw

    int whiteKingSquare;
    int blackKingSquare;

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
        legalMoves = MoveGen.generateLegalMoves(this, masks);

        // look for initial checks
        lookForChecks();
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

        // checks
        lookForChecks();
        updateKingSquares();
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
        } else if (move.flags == 2 || move.flags == 3) {
            SoundsPlayer.playCastleSound();
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
        legalMoves = MoveGen.generateLegalMoves(this, masks);
    }


    public HashMap<Integer, MoveList> getLegalMoves(){
        return this.legalMoves;
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


    private void lookForChecks() {
        // Squares attacked by the side who just moved
        long whiteAttackMask = getAttackMask(true);
        long blackAttackmask = getAttackMask(false);

        long whiteKingMask =  bitBoards[Piece.WK.ordinal()];
        long blackKingMask = bitBoards[Piece.BK.ordinal()];

        // Check for white being in check
        if ((whiteKingMask & blackAttackmask) != 0){
            whiteInCheck = true;
        }
        else {
            whiteInCheck = false;
        }

        // Check for black being in check
        if ((blackKingMask & whiteAttackMask) != 0){
            blackInCheck = true;
        }
        else{
            blackInCheck = false;
        }
    }


    public void lookForCheckmate() {
        if (gameOver) return;

        if (!whiteInCheck && !blackInCheck) return;

        if (whiteInCheck){
            if (legalMoves.get(whiteKingSquare).isEmpty()){
                System.out.println("Checkmate Black wins");
            }
        }

        if (blackInCheck){
            if (legalMoves.get(blackKingSquare).isEmpty()){
                System.out.println("Checkmate White wins");
            }
        }
    }


    private void  updateKingSquares(){
        whiteKingSquare = Utils.extractSquares(bitBoards[Piece.WK.ordinal()]).getFirst();
        blackKingSquare = Utils.extractSquares(bitBoards[Piece.BK.ordinal()]).getFirst();
    }


    /**
     * Engine / move-gen version of makeMove:
     * - moves pieces on bitboards
     * - handles captures
     * - handles castling rook move
     * - updates check + king squares
     * Does NOT:
     * - update castling "has moved" booleans
     * - update turnCounter or pseudoLegalMoves
     * - set lastMove / play sounds
     */
    public void makeMoveInternal(Move move) {
        Piece piece    = move.piece;
        Piece captured = move.capture;

        // 1. Move the main piece
        setBitboardBit(piece, move.from, false);
        setBitboardBit(piece, move.to, true);

        // 2. Remove captured piece (if any)
        if (captured != null) {
            setBitboardBit(captured, move.to, false);
        }

        // 3. Handle castling rook move (flags 2 = queenside, 3 = kingside)
        if (move.flags == 2 || move.flags == 3) {
            if (piece.isWhite()) {
                // White castling
                if (move.flags == 2) {
                    // White queenside: king e1->c1, rook a1->d1
                    setBitboardBit(Piece.WR, 0, false);
                    setBitboardBit(Piece.WR, 3, true);
                } else {
                    // White kingside: king e1->g1, rook h1->f1
                    setBitboardBit(Piece.WR, 7, false);
                    setBitboardBit(Piece.WR, 5, true);
                }
            } else {
                // Black castling
                if (move.flags == 2) {
                    // Black queenside: king e8->c8, rook a8->d8
                    setBitboardBit(Piece.BR, 56, false);
                    setBitboardBit(Piece.BR, 59, true);
                } else {
                    // Black kingside: king e8->g8, rook h8->f8
                    setBitboardBit(Piece.BR, 63, false);
                    setBitboardBit(Piece.BR, 61, true);
                }
            }
        }

        // 4. Recompute checks + king squares in the new position
        lookForChecks();
        updateKingSquares();
    }

    /**
     * Undo a move previously done with makeMoveInternal.
     * Must exactly reverse the operations there.
     */
    public void unmakeMoveInternal(Move move) {
        Piece piece    = move.piece;
        Piece captured = move.capture;

        // 1. If it was castling, move the rook back first
        if (move.flags == 2 || move.flags == 3) {
            if (piece.isWhite()) {
                if (move.flags == 2) {
                    // Undo white queenside: rook d1->a1
                    setBitboardBit(Piece.WR, 3, false);
                    setBitboardBit(Piece.WR, 0, true);
                } else {
                    // Undo white kingside: rook f1->h1
                    setBitboardBit(Piece.WR, 5, false);
                    setBitboardBit(Piece.WR, 7, true);
                }
            } else {
                if (move.flags == 2) {
                    // Undo black queenside: rook d8->a8
                    setBitboardBit(Piece.BR, 59, false);
                    setBitboardBit(Piece.BR, 56, true);
                } else {
                    // Undo black kingside: rook f8->h8
                    setBitboardBit(Piece.BR, 61, false);
                    setBitboardBit(Piece.BR, 63, true);
                }
            }
        }

        // 2. Move the piece back
        setBitboardBit(piece, move.to, false);
        setBitboardBit(piece, move.from, true);

        // 3. Restore captured piece, if there was one
        if (captured != null) {
            setBitboardBit(captured, move.to, true);
        }

        // 4. Recompute checks + king squares for the restored position
        lookForChecks();
        updateKingSquares();
    }

    /**
     *
     * @param depth how deep the move test will go
     * @return the numbers of successful moves
     */
    public int moveGenerationTest (int depth){
        MoveList largeMoveList;

        if (depth == 0){
            return 1;
        }

        HashMap<Integer, MoveList> moveListHashMap = MoveGen.generateLegalMoves(this, masks);
        int numPositions = 0;

        for (MoveList moveList : moveListHashMap.values()){
            for (Move move : moveList){
                makeMoveInternal(move);
                numPositions += moveGenerationTest(depth - 1);
                unmakeMoveInternal(move);
            }
        }
        return numPositions;
    }
}
