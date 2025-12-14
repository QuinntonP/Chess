package org.quinnton.chess.core;

import org.quinnton.chess.bot.Evaluate;

import java.util.Arrays;
import java.util.HashMap;

public class Board {
    public final Masks masks;
    public Evaluate evaluate;

    HashMap<Integer, MoveList> legalMoves;

    protected long[] bitBoards = new long[Piece.values().length];
    private int turnCounter = 0;

    // en-passant
    int enPassantSquare = -1;  // -1 = no en passant possible
    int prevEnPassantSquare = -1;

    // castling rights flags
    boolean whiteKingHasMoved = false;
    boolean blackKingHasMoved = false;
    boolean whiteKingRookHasMoved = false;
    boolean whiteQueenRookHasMoved = false;
    boolean blackKingRookHasMoved = false;
    boolean blackQueenRookHasMoved = false;

    // checks
    public boolean whiteInCheck;
    public boolean blackInCheck;

    // --- game state flags ---
    public boolean gameOver = false;
    boolean stalemate = false;
    Boolean winnerIsWhite = null; // null = no winner yet / draw

    int whiteKingSquare;
    int blackKingSquare;

    Move lastMove;
    Move lastWhiteMove;
    Move lastBlackMove;

    // mailbox: fast piece lookup (must be kept in sync with bitboards!)
    Piece[] mailbox = new Piece[64];

    public Board(Masks masks) {
        this.masks = masks;
    }

    public int getEnPassantSquare() {
        return enPassantSquare;
    }

    public void setEnPassantSquare(int sq) {
        this.enPassantSquare = sq;
    }

    public void loadFen(String fen) {
        // Clear any previous position
        Arrays.fill(bitBoards, 0L);
        Arrays.fill(mailbox, null);

        // Reset state that should not leak between positions
        enPassantSquare = -1;
        prevEnPassantSquare = -1;

        whiteInCheck = false;
        blackInCheck = false;

        gameOver = false;
        stalemate = false;
        winnerIsWhite = null;

        lastMove = null;
        lastWhiteMove = null;
        lastBlackMove = null;

        // --- Parse fields ---
        String[] fields = fen.trim().split("\\s+");
        if (fields.length < 1) throw new IllegalArgumentException("Empty FEN");

        String placement = fields[0];
        String sideToMove = (fields.length > 1) ? fields[1] : "w";
        String castling   = (fields.length > 2) ? fields[2] : "-";
        String epField    = (fields.length > 3) ? fields[3] : "-";

        // --- 1) Piece placement ---
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
                file += (c - '0');
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
            mailbox[idx] = piece;
            file++;
        }

        // --- 2) Side to move -> your turnCounter parity ---
        // getTurnCounter(): true = white to move, false = black to move
        if (sideToMove.equals("w")) {
            turnCounter = 0;
        } else if (sideToMove.equals("b")) {
            turnCounter = 1;
        } else {
            throw new IllegalArgumentException("Bad side-to-move field: " + sideToMove);
        }

        // --- 3) Castling rights ---
        // Default to "moved" (so NO castling) unless rights explicitly present.
        whiteKingHasMoved = true;
        blackKingHasMoved = true;
        whiteKingRookHasMoved = true;
        whiteQueenRookHasMoved = true;
        blackKingRookHasMoved = true;
        blackQueenRookHasMoved = true;

        if (!castling.equals("-")) {
            if (castling.contains("K") || castling.contains("Q")) whiteKingHasMoved = false;
            if (castling.contains("k") || castling.contains("q")) blackKingHasMoved = false;

            if (castling.contains("K")) whiteKingRookHasMoved = false;
            if (castling.contains("Q")) whiteQueenRookHasMoved = false;
            if (castling.contains("k")) blackKingRookHasMoved = false;
            if (castling.contains("q")) blackQueenRookHasMoved = false;
        }

        // --- 4) En-passant target square ---
        if (epField.equals("-")) {
            enPassantSquare = -1;
        } else {
            if (epField.length() != 2) throw new IllegalArgumentException("Bad en-passant field: " + epField);
            char fileChar = epField.charAt(0);
            char rankChar = epField.charAt(1);

            int epFile = fileChar - 'a';
            int epRank = rankChar - '1';

            if (epFile < 0 || epFile > 7 || epRank < 0 || epRank > 7) {
                throw new IllegalArgumentException("Bad en-passant square: " + epField);
            }

            enPassantSquare = epRank * 8 + epFile; // a1=0 indexing
        }

        // --- 5) King squares + legal moves + initial checks ---
        updateKingSquares();
        legalMoves = MoveGen.generateLegalMoves(this, masks);
        lookForChecks();

        evaluate = new Evaluate(this);
    }

    /**
     * UI/game move: updates bitboards + mailbox + special moves.
     * (SelectionController calls addTurnCounter() after this.)
     */
    public void makeMove(Move move) {
        Piece mover = move.piece;

        // save + clear EP by default
        prevEnPassantSquare = enPassantSquare;
        enPassantSquare = -1;

        // -------------------------
        // CAPTURE (mailbox is source of truth)
        // -------------------------
        if (move.flags == 5) {
            // en-passant capture: pawn behind destination
            int capSq = mover.isWhite() ? (move.to - 8) : (move.to + 8);
            Piece capPiece = mailbox[capSq];
            if (capPiece != null) {
                setBitboardBit(capPiece, capSq, false);
            }
            mailbox[capSq] = null;
        } else {
            Piece capPiece = mailbox[move.to];
            if (capPiece != null) {
                setBitboardBit(capPiece, move.to, false);
                mailbox[move.to] = null;
            }
        }

        // -------------------------
        // MOVE mover piece
        // -------------------------
        setBitboardBit(mover, move.from, false);
        mailbox[move.from] = null;

        Piece placed = (move.promo != null) ? move.promo : mover;
        setBitboardBit(placed, move.to, true);
        mailbox[move.to] = placed;

        // -------------------------
        // CASTLING rook move
        // flags: 2 = queen-side, 3 = king-side
        // -------------------------
        if (move.flags == 2 || move.flags == 3) {
            if (mover.isWhite()) {
                if (move.flags == 2) { // white O-O-O: a1->d1
                    setBitboardBit(Piece.WR, 0, false);
                    setBitboardBit(Piece.WR, 3, true);
                    mailbox[0] = null;
                    mailbox[3] = Piece.WR;
                } else {              // white O-O: h1->f1
                    setBitboardBit(Piece.WR, 7, false);
                    setBitboardBit(Piece.WR, 5, true);
                    mailbox[7] = null;
                    mailbox[5] = Piece.WR;
                }
            } else {
                if (move.flags == 2) { // black O-O-O: a8->d8
                    setBitboardBit(Piece.BR, 56, false);
                    setBitboardBit(Piece.BR, 59, true);
                    mailbox[56] = null;
                    mailbox[59] = Piece.BR;
                } else {               // black O-O: h8->f8
                    setBitboardBit(Piece.BR, 63, false);
                    setBitboardBit(Piece.BR, 61, true);
                    mailbox[63] = null;
                    mailbox[61] = Piece.BR;
                }
            }
        }

        // -------------------------
        // EP square only on pawn double push
        // -------------------------
        if ((mover == Piece.WP || mover == Piece.BP) && Math.abs(move.to - move.from) == 16) {
            enPassantSquare = (move.from + move.to) / 2;
        }

        // castling rights flags (based on what moved)
        checkCastlingPieces(move.from);

        updateKingSquares();
        lookForChecks();
    }

    public void setLastMove(Move move) {
        lastMove = move;
        if (move.piece.isWhite()) lastWhiteMove = move;
        else lastBlackMove = move;

        playMoveSound(move);
    }

    private void playMoveSound(Move move) {
        if (move.promo != null) {
            SoundsPlayer.playPromoteSound();
        } else if (move.capture != null) {
            SoundsPlayer.playCaptureSound();
        } else if (move.flags == 2 || move.flags == 3) {
            SoundsPlayer.playCastleSound();
        } else {
            SoundsPlayer.playMoveSelfSound();
        }
    }

    public Move getLastMove() { return lastMove; }
    public Move getLastWhiteMove() { return lastWhiteMove; }
    public Move getLastBlackMove() { return lastBlackMove; }

    // ------------------------------------------------------------
    // Bitboard + mailbox helpers
    // ------------------------------------------------------------

    public void setBitboardBit(Piece piece, int square, boolean set) {
        long mask = 1L << square;
        if (set) bitBoards[piece.ordinal()] |= mask;
        else bitBoards[piece.ordinal()] &= ~mask;
    }

    // mailbox-backed lookup (fast)
    public Piece getPieceAtSquare(int square) {
        return mailbox[square];
    }

    public long getAllWhitePieces() {
        long mask = 0L;
        Piece[] whites = { Piece.WK, Piece.WQ, Piece.WB, Piece.WR, Piece.WN, Piece.WP };
        for (Piece p : whites) mask |= bitBoards[p.ordinal()];
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
     * @return true if white to move, false if black to move
     */
    public boolean getTurnCounter() {
        return (turnCounter % 2) == 0;
    }

    public void addTurnCounter() {
        this.turnCounter++;
        legalMoves = MoveGen.generateLegalMoves(this, masks);
    }

    public HashMap<Integer, MoveList> getLegalMoves() {
        return this.legalMoves;
    }

    // ------------------------------------------------------------
    // Check / attack logic (unchanged)
    // ------------------------------------------------------------

    public long getAttackMask(boolean byWhite) {
        long mask = 0L;

        long bb = byWhite ? getAllWhitePieces() : getAllBlackPieces();
        HashMap<Integer, MoveList> allMoves = MoveGen.generatePseudoLegalMoves(this, masks, bb, false, true);

        for (MoveList moveList : allMoves.values()) {
            for (Move move : moveList) {
                int to = move.to;
                if (move.flags == 0) {
                    mask |= 1L << to;
                }
            }
        }

        return mask;
    }

    private void lookForChecks() {
        long whiteAttackMask = getAttackMask(true);
        long blackAttackMask = getAttackMask(false);

        long whiteKingMask = bitBoards[Piece.WK.ordinal()];
        long blackKingMask = bitBoards[Piece.BK.ordinal()];

        whiteInCheck = (whiteKingMask & blackAttackMask) != 0;
        blackInCheck = (blackKingMask & whiteAttackMask) != 0;
    }

    public void lookForCheckmate() {
        if (gameOver) return;
        if (!whiteInCheck && !blackInCheck) return;

        boolean noLegalMoves = (legalMoves == null || legalMoves.isEmpty());

        if (whiteInCheck && noLegalMoves) System.out.println("Checkmate Black wins");
        if (blackInCheck && noLegalMoves) System.out.println("Checkmate White wins");
    }

    private void updateKingSquares() {
        whiteKingSquare = Utils.extractSquares(bitBoards[Piece.WK.ordinal()]).getFirst();
        blackKingSquare = Utils.extractSquares(bitBoards[Piece.BK.ordinal()]).getFirst();
    }

    private void checkCastlingPieces(int square) {
        switch (square) {
            case 0  -> whiteQueenRookHasMoved = true;
            case 7  -> whiteKingRookHasMoved = true;
            case 56 -> blackQueenRookHasMoved = true;
            case 63 -> blackKingRookHasMoved = true;
            case 4  -> whiteKingHasMoved = true;
            case 60 -> blackKingHasMoved = true;
        }
    }

    // ------------------------------------------------------------
    // Engine make/unmake (MUST keep mailbox in sync)
    // ------------------------------------------------------------

    public void makeMoveInternal(Move move) {
        Piece mover = move.piece;

        move.prevEnPassantSquare = enPassantSquare;
        enPassantSquare = -1;

        // capture
        if (move.flags == 5) {
            int capSq = mover.isWhite() ? (move.to - 8) : (move.to + 8);
            Piece capPiece = mailbox[capSq];
            if (capPiece != null) setBitboardBit(capPiece, capSq, false);
            mailbox[capSq] = null;
        } else if (move.capture != null) {
            setBitboardBit(move.capture, move.to, false);
            mailbox[move.to] = null;
        } else {
            // if capture info isn't set, mailbox still might have something (safety)
            Piece capPiece = mailbox[move.to];
            if (capPiece != null) {
                setBitboardBit(capPiece, move.to, false);
                mailbox[move.to] = null;
            }
        }

        // move mover
        setBitboardBit(mover, move.from, false);
        mailbox[move.from] = null;

        Piece placed = (move.promo != null) ? move.promo : mover;
        setBitboardBit(placed, move.to, true);
        mailbox[move.to] = placed;

        // castling rook move
        if (move.flags == 2 || move.flags == 3) {
            if (mover.isWhite()) {
                if (move.flags == 2) { // a1->d1
                    setBitboardBit(Piece.WR, 0, false);
                    setBitboardBit(Piece.WR, 3, true);
                    mailbox[0] = null;
                    mailbox[3] = Piece.WR;
                } else {              // h1->f1
                    setBitboardBit(Piece.WR, 7, false);
                    setBitboardBit(Piece.WR, 5, true);
                    mailbox[7] = null;
                    mailbox[5] = Piece.WR;
                }
            } else {
                if (move.flags == 2) { // a8->d8
                    setBitboardBit(Piece.BR, 56, false);
                    setBitboardBit(Piece.BR, 59, true);
                    mailbox[56] = null;
                    mailbox[59] = Piece.BR;
                } else {               // h8->f8
                    setBitboardBit(Piece.BR, 63, false);
                    setBitboardBit(Piece.BR, 61, true);
                    mailbox[63] = null;
                    mailbox[61] = Piece.BR;
                }
            }
        }

        // EP square only on pawn double push
        if ((mover == Piece.WP || mover == Piece.BP) && Math.abs(move.to - move.from) == 16) {
            enPassantSquare = (move.from + move.to) / 2;
        }

        updateKingSquares();
        lookForChecks();
        turnCounter++;
    }

    public void unmakeMoveInternal(Move move) {
        Piece mover = move.piece;

        turnCounter--;

        // undo castling rook move first (so squares are free/accurate)
        if (move.flags == 2 || move.flags == 3) {
            if (mover.isWhite()) {
                if (move.flags == 2) { // d1->a1
                    setBitboardBit(Piece.WR, 3, false);
                    setBitboardBit(Piece.WR, 0, true);
                    mailbox[3] = null;
                    mailbox[0] = Piece.WR;
                } else {              // f1->h1
                    setBitboardBit(Piece.WR, 5, false);
                    setBitboardBit(Piece.WR, 7, true);
                    mailbox[5] = null;
                    mailbox[7] = Piece.WR;
                }
            } else {
                if (move.flags == 2) { // d8->a8
                    setBitboardBit(Piece.BR, 59, false);
                    setBitboardBit(Piece.BR, 56, true);
                    mailbox[59] = null;
                    mailbox[56] = Piece.BR;
                } else {               // f8->h8
                    setBitboardBit(Piece.BR, 61, false);
                    setBitboardBit(Piece.BR, 63, true);
                    mailbox[61] = null;
                    mailbox[63] = Piece.BR;
                }
            }
        }

        // remove placed piece from 'to'
        Piece placed = (move.promo != null) ? move.promo : mover;
        setBitboardBit(placed, move.to, false);
        mailbox[move.to] = null;

        // restore capture
        if (move.flags == 5) {
            int capSq = mover.isWhite() ? (move.to - 8) : (move.to + 8);
            Piece pawn = mover.isWhite() ? Piece.BP : Piece.WP;
            setBitboardBit(pawn, capSq, true);
            mailbox[capSq] = pawn;
        } else if (move.capture != null) {
            setBitboardBit(move.capture, move.to, true);
            mailbox[move.to] = move.capture;
        }

        // restore mover at from
        setBitboardBit(mover, move.from, true);
        mailbox[move.from] = mover;

        // restore EP square
        enPassantSquare = move.prevEnPassantSquare;

        updateKingSquares();
        lookForChecks();
    }

    // bitboard getter
    public long getBitboard(Piece piece) {
        return bitBoards[piece.ordinal()];
    }

    public Board copy() {
        Board b = new Board(this.masks);

        b.bitBoards = this.bitBoards.clone();
        b.mailbox = this.mailbox.clone();

        b.turnCounter = this.turnCounter;

        b.enPassantSquare = this.enPassantSquare;
        b.prevEnPassantSquare = this.prevEnPassantSquare;

        b.whiteKingHasMoved = this.whiteKingHasMoved;
        b.blackKingHasMoved = this.blackKingHasMoved;
        b.whiteKingRookHasMoved = this.whiteKingRookHasMoved;
        b.whiteQueenRookHasMoved = this.whiteQueenRookHasMoved;
        b.blackKingRookHasMoved = this.blackKingRookHasMoved;
        b.blackQueenRookHasMoved = this.blackQueenRookHasMoved;

        b.whiteInCheck = this.whiteInCheck;
        b.blackInCheck = this.blackInCheck;

        b.gameOver = this.gameOver;
        b.stalemate = this.stalemate;
        b.winnerIsWhite = this.winnerIsWhite;

        b.whiteKingSquare = this.whiteKingSquare;
        b.blackKingSquare = this.blackKingSquare;

        b.lastMove = null;
        b.lastWhiteMove = null;
        b.lastBlackMove = null;

        b.legalMoves = null; // bot regenerates when needed
        b.evaluate = new Evaluate(b);

        return b;
    }
}
