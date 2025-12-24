package org.quinnton.chess.core;

import org.quinnton.chess.bot.Evaluate;

import java.util.Arrays;

public class Board {

    public final Masks masks;
    public Evaluate evaluate;

    // ------------------------------------------------------------
    // Move storage (flat, no HashMap)
    // ------------------------------------------------------------
    public static final int MAX_MOVES = 256;
    private final int[] legalMoves = new int[MAX_MOVES];
    private int legalMoveCount = 0;

    // ------------------------------------------------------------
    // Bitboards + mailbox
    // ------------------------------------------------------------
    protected long[] bitBoards = new long[Piece.values().length];
    // mailbox: fast piece lookup (must be kept in sync with bitboards!)
    Piece[] mailbox = new Piece[64];

    // ------------------------------------------------------------
    // Turn
    // ------------------------------------------------------------
    private int turnCounter = 0;

    // ------------------------------------------------------------
    // en-passant
    // ------------------------------------------------------------
    int enPassantSquare = -1;      // -1 = no en passant possible

    // ------------------------------------------------------------
    // castling rights flags (same as yours)
    // ------------------------------------------------------------
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

    // last moves (encoded int); -1 means none
    int lastMove = -1;
    int lastWhiteMove = -1;
    int lastBlackMove = -1;

    // ------------------------------------------------------------
    // Undo stacks for INTERNAL make/unmake (search/perft)
    // ------------------------------------------------------------
    private static final int MAX_PLY = 2048;

    private final int[] undoEp = new int[MAX_PLY];

    // store captured piece id (0 = none). For EP we store pawn id.
    private final int[] undoCapId = new int[MAX_PLY];

    // store castling flags packed into bits
    // bit 0: wK moved, 1: bK moved, 2: wKR moved, 3: wQR moved, 4: bKR moved, 5: bQR moved
    private final int[] undoCastle = new int[MAX_PLY];

    private int ply = 0;

    public Board(Masks masks) {
        this.masks = masks;
    }

    // ------------------------------------------------------------
    // Basic getters
    // ------------------------------------------------------------
    public int getEnPassantSquare() {
        return enPassantSquare;
    }

    public void setEnPassantSquare(int sq) {
        this.enPassantSquare = sq;
    }

    /**
     * @return true if white to move, false if black to move
     */
    public boolean getTurnCounter() {
        return (turnCounter % 2) == 0;
    }

    public int[] getLegalMovesArray() {
        return legalMoves;
    }

    public int getLegalMoveCount() {
        return legalMoveCount;
    }

    // ------------------------------------------------------------
    // FEN
    // ------------------------------------------------------------
    public void loadFen(String fen) {
        Arrays.fill(bitBoards, 0L);
        Arrays.fill(mailbox, null);

        enPassantSquare = -1;

        whiteInCheck = false;
        blackInCheck = false;

        gameOver = false;
        stalemate = false;
        winnerIsWhite = null;

        lastMove = -1;
        lastWhiteMove = -1;
        lastBlackMove = -1;

        ply = 0;

        String[] fields = fen.trim().split("\\s+");
        if (fields.length < 1) throw new IllegalArgumentException("Empty FEN");

        String placement  = fields[0];
        String sideToMove = (fields.length > 1) ? fields[1] : "w";
        String castling   = (fields.length > 2) ? fields[2] : "-";
        String epField    = (fields.length > 3) ? fields[3] : "-";

        // --- 1) Piece placement ---
        int rank = 7;
        int file = 0;

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

            int idx = rank * 8 + file;
            setBitboardBit(piece, idx, true);
            mailbox[idx] = piece;
            file++;
        }

        // --- 2) Side to move ---
        if (sideToMove.equals("w")) {
            turnCounter = 0;
        } else if (sideToMove.equals("b")) {
            turnCounter = 1;
        } else {
            throw new IllegalArgumentException("Bad side-to-move field: " + sideToMove);
        }

        // --- 3) Castling rights ---
        // Default to moved=true (no castling) unless rights present
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

        // --- 4) En-passant square ---
        if (epField.equals("-")) {
            enPassantSquare = -1;
        } else {
            if (epField.length() != 2) throw new IllegalArgumentException("Bad en-passant field: " + epField);
            int epFile = epField.charAt(0) - 'a';
            int epRank = epField.charAt(1) - '1';
            if (epFile < 0 || epFile > 7 || epRank < 0 || epRank > 7) {
                throw new IllegalArgumentException("Bad en-passant square: " + epField);
            }
            enPassantSquare = epRank * 8 + epFile;
        }

        setKingSquares();
        lookForChecks();

        // initial legal moves
        legalMoveCount = MoveGen.generateLegalMovesFlat(this, masks, legalMoves);

        evaluate = new Evaluate(this);
    }

    // ------------------------------------------------------------
    // UI / Game move (encoded int)
    // NOTE: SelectionController should call addTurnCounter() after this, as before.
    // ------------------------------------------------------------
    public void makeMove(int m) {
        Piece mover = Move.piece(m);
        int from = Move.from(m);
        int to = Move.to(m);
        int flags = Move.flags(m);

        // save + clear EP by default (UI path can just store prev locally if needed)
        int prevEp = enPassantSquare;
        enPassantSquare = -1;

        // -------------------------
        // CAPTURE
        // -------------------------
        if (flags == Move.FLAG_EN_PASSANT) {
            int capSq = mover.isWhite() ? (to - 8) : (to + 8);
            Piece capPiece = mailbox[capSq];
            if (capPiece != null) setBitboardBit(capPiece, capSq, false);
            mailbox[capSq] = null;
        } else {
            Piece capPiece = mailbox[to];
            if (capPiece != null) {
                setBitboardBit(capPiece, to, false);
                mailbox[to] = null;
            }
        }

        // -------------------------
        // MOVE mover piece
        // -------------------------
        setBitboardBit(mover, from, false);
        mailbox[from] = null;

        Piece promo = Move.promo(m);
        Piece placed = (promo != null) ? promo : mover;

        setBitboardBit(placed, to, true);
        mailbox[to] = placed;

        // -------------------------
        // CASTLING rook move
        // -------------------------
        if (flags == Move.FLAG_CASTLE_QS || flags == Move.FLAG_CASTLE_KS) {
            if (mover.isWhite()) {
                if (flags == Move.FLAG_CASTLE_QS) { // a1->d1
                    setBitboardBit(Piece.WR, 0, false);
                    setBitboardBit(Piece.WR, 3, true);
                    mailbox[0] = null;
                    mailbox[3] = Piece.WR;
                } else { // h1->f1
                    setBitboardBit(Piece.WR, 7, false);
                    setBitboardBit(Piece.WR, 5, true);
                    mailbox[7] = null;
                    mailbox[5] = Piece.WR;
                }
            } else {
                if (flags == Move.FLAG_CASTLE_QS) { // a8->d8
                    setBitboardBit(Piece.BR, 56, false);
                    setBitboardBit(Piece.BR, 59, true);
                    mailbox[56] = null;
                    mailbox[59] = Piece.BR;
                } else { // h8->f8
                    setBitboardBit(Piece.BR, 63, false);
                    setBitboardBit(Piece.BR, 61, true);
                    mailbox[63] = null;
                    mailbox[61] = Piece.BR;
                }
            }
        }

        // -------------------------
        // EP square only on pawn double push
        // (your original logic: pawn moved 16 squares)
        // -------------------------
        if ((mover == Piece.WP || mover == Piece.BP) && Math.abs(to - from) == 16) {
            enPassantSquare = (from + to) / 2;
        } else {
            enPassantSquare = -1;
        }

        // castling rights flags (based on what moved)
        checkCastlingPieces(from);

        // restore prevEp not used here (kept in internal undo stack instead)
        // kept line to avoid “unused” confusion:
        @SuppressWarnings("unused")
        int _unusedPrevEp = prevEp;

        if (mover == Piece.WK){
            whiteKingSquare = to;
        }
        if (mover == Piece.BK){
            blackKingSquare = to;
        }

        lookForChecks();

        System.out.println("Checking whiteking square" + whiteKingSquare);
    }

    public void setLastMove(int m) {
        lastMove = m;
        Piece p = Move.piece(m);
        if (p != null && p.isWhite()) lastWhiteMove = m;
        else lastBlackMove = m;

        playMoveSound(m);
    }

    private void playMoveSound(int m) {
        if (Move.promoId(m) != 0) {
            SoundsPlayer.playPromoteSound();
        } else if (Move.capId(m) != 0 || Move.flags(m) == Move.FLAG_EN_PASSANT) {
            SoundsPlayer.playCaptureSound();
        } else if (Move.flags(m) == Move.FLAG_CASTLE_QS || Move.flags(m) == Move.FLAG_CASTLE_KS) {
            SoundsPlayer.playCastleSound();
        } else {
            SoundsPlayer.playMoveSelfSound();
        }
    }

    public int getLastMove() { return lastMove; }
    public int getLastWhiteMove() { return lastWhiteMove; }
    public int getLastBlackMove() { return lastBlackMove; }

    // ------------------------------------------------------------
    // Bitboard + mailbox helpers
    // ------------------------------------------------------------
    public void setBitboardBit(Piece piece, int square, boolean set) {
        long mask = 1L << square;
        if (set) bitBoards[piece.ordinal()] |= mask;
        else bitBoards[piece.ordinal()] &= ~mask;
    }

    public Piece getPieceAtSquare(int square) {
        return mailbox[square];
    }

    public long getAllWhitePieces() {
        long mask = 0L;
        mask |= bitBoards[Piece.WK.ordinal()];
        mask |= bitBoards[Piece.WQ.ordinal()];
        mask |= bitBoards[Piece.WB.ordinal()];
        mask |= bitBoards[Piece.WR.ordinal()];
        mask |= bitBoards[Piece.WN.ordinal()];
        mask |= bitBoards[Piece.WP.ordinal()];
        return mask;
    }

    public long getAllBlackPieces() {
        long mask = 0L;
        mask |= bitBoards[Piece.BK.ordinal()];
        mask |= bitBoards[Piece.BQ.ordinal()];
        mask |= bitBoards[Piece.BB.ordinal()];
        mask |= bitBoards[Piece.BR.ordinal()];
        mask |= bitBoards[Piece.BN.ordinal()];
        mask |= bitBoards[Piece.BP.ordinal()];
        return mask;
    }

    public long getAllPieces() {
        return getAllWhitePieces() | getAllBlackPieces();
    }

    public long getAllPawns() {
        return bitBoards[Piece.WP.ordinal()] | bitBoards[Piece.BP.ordinal()];
    }

    public void addTurnCounter() {
        this.turnCounter++;
        legalMoveCount = MoveGen.generateLegalMovesFlat(this, masks, legalMoves);
    }


    public long getAttackMask(boolean byWhite) {
        long mask = 0L;

        long bb = byWhite ? getAllWhitePieces() : getAllBlackPieces();

        // Attack mask does NOT include castling; pawnAttackMask=true means:
        // pawn generators emit diagonal attacks even if empty.
        int[] moves = new int[MAX_MOVES];

        while (bb != 0) {
            int from = Long.numberOfTrailingZeros(bb);
            bb &= bb - 1;

            Piece p = getPieceAtSquare(from);
            if (p == null) continue;
            if (p.isWhite() != byWhite) continue;

            int count = MoveGen.generateInto(this, from, p, masks, false, true, moves, 0);

            for (int i = 0; i < count; i++) {
                int to = Move.to(moves[i]);
                mask |= 1L << to;
            }
        }

        return mask;
    }

//    private void lookForChecks() {
//        long whiteAttackMask = getAttackMask(true);
//        long blackAttackMask = getAttackMask(false);
//
//        long whiteKingMask = bitBoards[Piece.WK.ordinal()];
//        long blackKingMask = bitBoards[Piece.BK.ordinal()];
//
//        whiteInCheck = (whiteKingMask & blackAttackMask) != 0;
//        blackInCheck = (blackKingMask & whiteAttackMask) != 0;
//    }

    private void lookForChecks() {
        whiteInCheck = isSquareAttacked(whiteKingSquare, true);
        blackInCheck = isSquareAttacked(blackKingSquare, false);
    }

    // test
    private boolean getKingInCheck(boolean isWhite){
        if (isWhite){
            return whiteInCheck;
        }
        else{
            return blackInCheck;
        }
    }

    /**
     * Checks attack masks of all pieces on the opposing side to see if they are attacking that square
     * @param sq the square being attacked
     * @param isWhite if you want to check for white or black
     * @return true means it is being attacked false means it is not
     */
    public boolean isSquareAttacked(int sq, boolean isWhite){
        long occ = getAllPieces();

        long b = 1L << sq;

        // Black and White Pawns
        if (isWhite) {
            long pawns = getBitboard(Piece.BP);

            long attacks =
                    ((b & Masks.NOT_FILE_H) << 7) |   // attacker from sq-7
                            ((b & Masks.NOT_FILE_A) << 9);    // attacker from sq-9

            if ((attacks & pawns) != 0) {
//                System.out.println("In check from black pawn");
                return true;
            }
        } else { // is black king in check? -> attacked by WHITE pawns
            long pawns = getBitboard(Piece.WP);

            long attacks =
                    ((b & Masks.NOT_FILE_A) >>> 7) |    // attacker from sq+7
                            ((b & Masks.NOT_FILE_H) >>> 9);     // attacker from sq+9

            if ((attacks & pawns) != 0) return true;
        }


        if (isWhite){
            // Knight
            long knightAttacks = Masks.knightMoves.get(sq);
            if ((knightAttacks & getBitboard(Piece.BN)) != 0){
//                System.out.println("In check from Knight");
                return true;
            }

            // Kings
            long kingAttacks = Masks.kingMoves.get(sq);
            if ((kingAttacks & getBitboard(Piece.BK)) != 0){
//                System.out.println("In check from Black king");
                return true;
            }

            // Bishop and Queen diag
            long bSubset = occ & BishopMoveMasks.bishopBlockerMask(sq);
            long bishopAttacks = masks.getBishopMoves(sq, bSubset);

            if ((bishopAttacks & getBitboard(Piece.BB)) != 0 || (bishopAttacks & getBitboard(Piece.BQ)) != 0){
//                System.out.println("In check from Queen or bishop diag");
                return true;
            }

            // Rook and Queen straight
            long rSubset = occ & RookMoveMasks.rookBlockerMask(sq);
            long rookAttacks = masks.getRookMoves(sq, rSubset);

            if ((rookAttacks & getBitboard(Piece.BR)) != 0 || (rookAttacks & getBitboard(Piece.BQ)) != 0){
//                System.out.println("In check from Rook or Queen straight");
                return true;
            }
        }
        else{
            // Knight
            long knightAttacks = Masks.knightMoves.get(sq);
            if ((knightAttacks & getBitboard(Piece.WN)) != 0) {
//                System.out.println("In check from Knight");
                return true;
            }

            // King
            long kingAttacks = Masks.kingMoves.get(sq);
            if ((kingAttacks & getBitboard(Piece.WK)) != 0) {
//                System.out.println("In check from White king");
                return true;
            }

            // Bishop and Queen (diagonals)
            long bSubset = occ & BishopMoveMasks.bishopBlockerMask(sq);
            long bishopAttacks = masks.getBishopMoves(sq, bSubset);

            if ((bishopAttacks & getBitboard(Piece.WB)) != 0 ||
                    (bishopAttacks & getBitboard(Piece.WQ)) != 0) {
//                System.out.println("In check from Queen or bishop diag");
                return true;
            }

            // Rook and Queen (ranks/files)
            long rSubset = occ & RookMoveMasks.rookBlockerMask(sq);
            long rookAttacks = masks.getRookMoves(sq, rSubset);

            if ((rookAttacks & getBitboard(Piece.WR)) != 0 ||
                    (rookAttacks & getBitboard(Piece.WQ)) != 0) {
//                System.out.println("In check from Rook or Queen straight");
                return true;
            }
        }



        return false;
    }


    public void lookForCheckmate() {
        if (gameOver) return;
        if (!whiteInCheck && !blackInCheck) return;

        boolean noLegalMoves = (legalMoveCount == 0);

        if (whiteInCheck && noLegalMoves) System.out.println("Checkmate Black wins");
        if (blackInCheck && noLegalMoves) System.out.println("Checkmate White wins");
    }


    private void setKingSquares() {
        long wk = bitBoards[Piece.WK.ordinal()];
        long bk = bitBoards[Piece.BK.ordinal()];
        whiteKingSquare = (wk != 0) ? Long.numberOfTrailingZeros(wk) : -1;
        blackKingSquare = (bk != 0) ? Long.numberOfTrailingZeros(bk) : -1;
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
    // Engine make/unmake (encoded int). Keeps mailbox in sync.
    // Uses undo stacks instead of storing prev state in Move objects.
    // ------------------------------------------------------------
    public void makeMoveInternal(int m) {
        if (ply >= MAX_PLY) throw new IllegalStateException("Undo stack overflow");

        // save undo state
        undoEp[ply] = enPassantSquare;
        undoCastle[ply] = packCastleFlags();

        Piece mover = Move.piece(m);
        int from = Move.from(m);
        int to = Move.to(m);
        int flags = Move.flags(m);

        // clear EP by default
        enPassantSquare = -1;

        // capture handling (record captured id for undo)
        int capturedId = 0;

        if (flags == Move.FLAG_EN_PASSANT) {
            int capSq = mover.isWhite() ? (to - 8) : (to + 8);
            Piece capPiece = mailbox[capSq];
            capturedId = (capPiece == null) ? 0 : (capPiece.ordinal() + 1);

            if (capPiece != null) setBitboardBit(capPiece, capSq, false);
            mailbox[capSq] = null;
        } else {
            Piece capPiece = mailbox[to];
            capturedId = (capPiece == null) ? 0 : (capPiece.ordinal() + 1);

            if (capPiece != null) {
                setBitboardBit(capPiece, to, false);
                mailbox[to] = null;
            }
        }

        undoCapId[ply] = capturedId;

        // move mover off from
        setBitboardBit(mover, from, false);
        mailbox[from] = null;

        // place promo or mover on to
        Piece promo = Move.promo(m);
        Piece placed = (promo != null) ? promo : mover;
        setBitboardBit(placed, to, true);
        mailbox[to] = placed;

        // castling rook move
        if (flags == Move.FLAG_CASTLE_QS || flags == Move.FLAG_CASTLE_KS) {
            if (mover.isWhite()) {
                if (flags == Move.FLAG_CASTLE_QS) { // a1->d1
                    setBitboardBit(Piece.WR, 0, false);
                    setBitboardBit(Piece.WR, 3, true);
                    mailbox[0] = null;
                    mailbox[3] = Piece.WR;
                } else { // h1->f1
                    setBitboardBit(Piece.WR, 7, false);
                    setBitboardBit(Piece.WR, 5, true);
                    mailbox[7] = null;
                    mailbox[5] = Piece.WR;
                }
            } else {
                if (flags == Move.FLAG_CASTLE_QS) { // a8->d8
                    setBitboardBit(Piece.BR, 56, false);
                    setBitboardBit(Piece.BR, 59, true);
                    mailbox[56] = null;
                    mailbox[59] = Piece.BR;
                } else { // h8->f8
                    setBitboardBit(Piece.BR, 63, false);
                    setBitboardBit(Piece.BR, 61, true);
                    mailbox[63] = null;
                    mailbox[61] = Piece.BR;
                }
            }
        }

        // EP square only on pawn double push
        if ((mover == Piece.WP || mover == Piece.BP) && Math.abs(to - from) == 16) {
            enPassantSquare = (from + to) / 2;
        }

        // update castling rights if king/rook moved
        checkCastlingPieces(from);

        if (mover == Piece.WK){
            whiteKingSquare = to;
        }

        if (mover == Piece.BK){
            blackKingSquare = to;
        }

        lookForChecks();

        turnCounter++;
        ply++;
    }

    public void unmakeMoveInternal(int m) {
        ply--;
        if (ply < 0) throw new IllegalStateException("Undo stack underflow");

        turnCounter--;

        Piece mover = Move.piece(m);
        int from = Move.from(m);
        int to = Move.to(m);
        int flags = Move.flags(m);

        // restore castling flags + EP at end, but we need them packed now
        int prevEp = undoEp[ply];
        int prevCastle = undoCastle[ply];
        int capturedId = undoCapId[ply];

        // undo castling rook move first
        if (flags == Move.FLAG_CASTLE_QS || flags == Move.FLAG_CASTLE_KS) {
            if (mover.isWhite()) {
                if (flags == Move.FLAG_CASTLE_QS) { // d1->a1
                    setBitboardBit(Piece.WR, 3, false);
                    setBitboardBit(Piece.WR, 0, true);
                    mailbox[3] = null;
                    mailbox[0] = Piece.WR;
                } else { // f1->h1
                    setBitboardBit(Piece.WR, 5, false);
                    setBitboardBit(Piece.WR, 7, true);
                    mailbox[5] = null;
                    mailbox[7] = Piece.WR;
                }
            } else {
                if (flags == Move.FLAG_CASTLE_QS) { // d8->a8
                    setBitboardBit(Piece.BR, 59, false);
                    setBitboardBit(Piece.BR, 56, true);
                    mailbox[59] = null;
                    mailbox[56] = Piece.BR;
                } else { // f8->h8
                    setBitboardBit(Piece.BR, 61, false);
                    setBitboardBit(Piece.BR, 63, true);
                    mailbox[61] = null;
                    mailbox[63] = Piece.BR;
                }
            }
        }

        // remove placed piece from 'to'
        Piece promo = Move.promo(m);
        Piece placed = (promo != null) ? promo : mover;
        setBitboardBit(placed, to, false);
        mailbox[to] = null;

        // restore mover at from
        setBitboardBit(mover, from, true);
        mailbox[from] = mover;

        // restore capture
        if (capturedId != 0) {
            Piece capPiece = Piece.values()[capturedId - 1];

            if (flags == Move.FLAG_EN_PASSANT) {
                int capSq = mover.isWhite() ? (to - 8) : (to + 8);
                setBitboardBit(capPiece, capSq, true);
                mailbox[capSq] = capPiece;
            } else {
                setBitboardBit(capPiece, to, true);
                mailbox[to] = capPiece;
            }
        }

        // restore EP + castling flags
        enPassantSquare = prevEp;
        unpackCastleFlags(prevCastle);

        if (mover == Piece.WK){
            whiteKingSquare = from;
        }

        if (mover == Piece.BK){
            blackKingSquare = from;
        }

        lookForChecks();
    }

    private int packCastleFlags() {
        int x = 0;
        if (whiteKingHasMoved) x |= 1;
        if (blackKingHasMoved) x |= 2;
        if (whiteKingRookHasMoved) x |= 4;
        if (whiteQueenRookHasMoved) x |= 8;
        if (blackKingRookHasMoved) x |= 16;
        if (blackQueenRookHasMoved) x |= 32;
        return x;
    }

    private void unpackCastleFlags(int x) {
        whiteKingHasMoved = (x & 1) != 0;
        blackKingHasMoved = (x & 2) != 0;
        whiteKingRookHasMoved = (x & 4) != 0;
        whiteQueenRookHasMoved = (x & 8) != 0;
        blackKingRookHasMoved = (x & 16) != 0;
        blackQueenRookHasMoved = (x & 32) != 0;
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

        b.lastMove = -1;
        b.lastWhiteMove = -1;
        b.lastBlackMove = -1;

        b.legalMoveCount = 0;
        // legalMoves array is already allocated in constructor

        b.evaluate = new Evaluate(b);

        return b;
    }
}
