package org.quinnton.chess.core;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class MoveGen {

    public static MoveList generate(Board board, int curSquare, Piece firstPiece, Masks masks, boolean includeCastling){
        MoveList list;

        list = switch (firstPiece) {
            case WP, BP -> genPawns(board, curSquare, firstPiece.white);
            case WN, BN -> genKnights(board, curSquare, firstPiece.white, masks);
            case WB, BB -> genBishops(board, curSquare, firstPiece.white, masks);
            case WR, BR -> genRooks(board, curSquare, firstPiece.white, masks);
            case WQ, BQ -> genQueens(board, curSquare, firstPiece.white, masks);
            case WK, BK -> genKings(board, curSquare, firstPiece.white, masks, includeCastling);
        };

        return  list;
    }

    private static MoveList genKnights(Board board, int curSquare, boolean isWhite, Masks masks) {
        MoveList list = new MoveList();
        long own = isWhite ? board.getAllWhitePieces() : board.getAllBlackPieces();
        long enemy = isWhite ? board.getAllBlackPieces() : board.getAllWhitePieces();

        long targets = masks.getKnightMoves(curSquare) & ~own; // can move to empty or enemy
        Piece mover = isWhite ? Piece.WN : Piece.BN;

        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            targets &= targets - 1;

            Piece captured = null;
            if (((enemy >>> to) & 1L) != 0) {
                // Only then pay the cost to resolve exact piece:
                captured = board.getPieceAtSquare(to);
            }
            list.add(new Move(curSquare, to, mover, captured, null, 0));
        }
        return list;
    }


    private static MoveList genKings(Board board, int curSquare, boolean isWhite, Masks masks, boolean includeCastling) {
        MoveList list = new MoveList();
        long own = isWhite ? board.getAllWhitePieces() : board.getAllBlackPieces();
        long enemy = isWhite ? board.getAllBlackPieces() : board.getAllWhitePieces();

        long targets = masks.getKingMoves(curSquare) & ~own;
        Piece mover = isWhite ? Piece.WK : Piece.BK;

        // castling
        if (includeCastling) {
            list = kingCastling(list, board, curSquare, isWhite);
        }

        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            targets &= targets - 1;

            Piece captured = ((enemy >>> to) & 1L) != 0 ? board.getPieceAtSquare(to) : null;
            list.add(new Move(curSquare, to, mover, captured, null, 0));
        }
        return list;
    }


    private static MoveList kingCastling(MoveList moveList, Board board, int curSquare, boolean isWhite) {
        long occupied   = board.getAllPieces();
        long attackMask = board.getAttackMask(!isWhite); // all squares attacked by the *opponent*

        if (isWhite) {
            // King must be on starting square and unmoved
            if (board.whiteKingHasMoved || curSquare != 4) {
                return moveList;
            }

            // ---------- White king-side castling (e1 -> g1) ----------
            if (!board.whiteKingRookHasMoved) {
                boolean emptyF1 = (occupied & (1L << 5)) == 0;
                boolean emptyG1 = (occupied & (1L << 6)) == 0;

                // e1, f1, g1 must NOT be attacked
                boolean safeE1 = (attackMask & (1L << 4)) == 0;
                boolean safeF1 = (attackMask & (1L << 5)) == 0;
                boolean safeG1 = (attackMask & (1L << 6)) == 0;

                if (emptyF1 && emptyG1 && safeE1 && safeF1 && safeG1) {
                    moveList.add(new Move(
                            4, 6,
                            Piece.WK,
                            null,
                            null,
                            3
                    ));
                }
            }

            // ---------- White queen-side castling (e1 -> c1) ----------
            if (!board.whiteQueenRookHasMoved) {
                boolean emptyB1 = (occupied & (1L << 1)) == 0;
                boolean emptyC1 = (occupied & (1L << 2)) == 0;
                boolean emptyD1 = (occupied & (1L << 3)) == 0;

                // e1, d1, c1 must NOT be attacked
                boolean safeE1 = (attackMask & (1L << 4)) == 0;
                boolean safeD1 = (attackMask & (1L << 3)) == 0;
                boolean safeC1 = (attackMask & (1L << 2)) == 0;

                if (emptyB1 && emptyC1 && emptyD1 && safeE1 && safeD1 && safeC1) {
                    moveList.add(new Move(
                            4, 2,
                            Piece.WK,
                            null,
                            null,
                            2
                    ));
                }
            }
        } else {
            // ---------- Black ----------
            if (board.blackKingHasMoved || curSquare != 60) {
                return moveList;
            }

            // ---------- Black king-side castling (e8 -> g8) ----------
            if (!board.blackKingRookHasMoved) {
                boolean emptyF8 = (occupied & (1L << 61)) == 0;
                boolean emptyG8 = (occupied & (1L << 62)) == 0;

                // e8, f8, g8 must NOT be attacked
                boolean safeE8 = (attackMask & (1L << 60)) == 0;
                boolean safeF8 = (attackMask & (1L << 61)) == 0;
                boolean safeG8 = (attackMask & (1L << 62)) == 0;

                if (emptyF8 && emptyG8 && safeE8 && safeF8 && safeG8) {
                    moveList.add(new Move(
                            60, 62,
                            Piece.BK,
                            null,
                            null,
                            3
                    ));
                }
            }

            // ---------- Black queen-side castling (e8 -> c8) ----------
            if (!board.blackQueenRookHasMoved) {
                boolean emptyB8 = (occupied & (1L << 57)) == 0;
                boolean emptyC8 = (occupied & (1L << 58)) == 0;
                boolean emptyD8 = (occupied & (1L << 59)) == 0;

                // e8, d8, c8 must NOT be attacked
                boolean safeE8 = (attackMask & (1L << 60)) == 0;
                boolean safeD8 = (attackMask & (1L << 59)) == 0;
                boolean safeC8 = (attackMask & (1L << 58)) == 0;

                if (emptyB8 && emptyC8 && emptyD8 && safeE8 && safeD8 && safeC8) {
                    moveList.add(new Move(
                            60, 58,
                            Piece.BK,
                            null,
                            null,
                            2
                    ));
                }
            }
        }

        return moveList;
    }




    private static MoveList genPawns(Board board, int curSquare, boolean isWhite){
        int rank = curSquare / 8;

        MoveList list = new MoveList();

        long mask;

        long own   = isWhite ? board.getAllWhitePieces() : board.getAllBlackPieces();
        long enemy = isWhite ? board.getAllBlackPieces() : board.getAllWhitePieces();

        long occupied = board.getAllPieces();
        long attackTargets = 0L;
        long movementTargets = 0L;

        Piece mover = isWhite ? Piece.WP : Piece.BP;

        int direction = 1;

        if(!isWhite){
            direction = -1;
        }

        // move 1 forward
        mask = 1L << (8 * direction + curSquare);
        if ((mask & occupied) == 0){
            movementTargets |= mask;

            //  move forward 2
            mask = 1L << (16 * direction + curSquare);
            if ((mask & occupied) == 0 && (rank == 1 || rank == 6)){
                movementTargets |= mask;
            }
        }

        // left capture
        mask = 1L << (7 * direction + curSquare);
        if ((mask & enemy) != 0){
            attackTargets |= mask;
        }

        // right capture
        mask = 1L << (9 * direction + curSquare);
        if ((mask & enemy) != 0){
            attackTargets |= mask;
        }

        // dang enpassant
        long enemyPawns = enemy & board.getAllPawns();

        Move myPreviousMove = isWhite ? board.getLastWhiteMove() : board.getLastBlackMove();

        if (((isWhite && rank == 4) || (!isWhite && rank == 3)) && myPreviousMove.to == curSquare){
            // right
            mask = 1L << curSquare + 1;
            if ((enemyPawns & mask) != 0){
                attackTargets |= mask;
            }

            // left
            mask = 1L << curSquare -1;
            if ((enemyPawns & mask) != 0){
                attackTargets |= mask;
            }

        }

        while (attackTargets != 0) {
            int to = Long.numberOfTrailingZeros(attackTargets);
            attackTargets &= attackTargets - 1;

            Piece captured = ((enemy >>> to) & 1L) != 0 ? board.getPieceAtSquare(to) : null;
            list.add(new Move(curSquare, to, mover, captured, null, 0));
        }

        while (movementTargets != 0){
            int to = Long.numberOfTrailingZeros(movementTargets);
            movementTargets &= movementTargets - 1;

            Piece captured = ((enemy >>> to) & 1L) != 0 ? board.getPieceAtSquare(to) : null;
            list.add(new Move(curSquare, to, mover, captured, null, 1));
        }

        return list;
    }

    private static MoveList genBishops(Board board, int curSquare, boolean isWhite, Masks masks){
        MoveList list = new MoveList();

        long own   = isWhite ? board.getAllWhitePieces() : board.getAllBlackPieces();
        long enemy = isWhite ? board.getAllBlackPieces() : board.getAllWhitePieces();

        long occupied = board.getAllPieces();

        long mask   = BishopMoveMasks.bishopBlockerMask(curSquare);
        long subset = occupied & mask;  // relevant blockers only

        long targets = masks.getBishopMoves(curSquare, subset);

        // Remove own pieces
        targets &= ~own;

        Piece mover = isWhite ? Piece.WB : Piece.BB;
        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            targets &= targets - 1;

            Piece captured = ((enemy >>> to) & 1L) != 0 ? board.getPieceAtSquare(to) : null;
            list.add(new Move(curSquare, to, mover, captured, null, 0));
        }

        return list;
    }

    private static MoveList genQueens(Board board, int curSquare, boolean isWhite, Masks masks){
        MoveList list = new MoveList();

        long own   = isWhite ? board.getAllWhitePieces() : board.getAllBlackPieces();
        long enemy = isWhite ? board.getAllBlackPieces() : board.getAllWhitePieces();

        long occupied = board.getAllPieces();

        long bishopMask   = BishopMoveMasks.bishopBlockerMask(curSquare);
        long rookMask     = RookMoveMasks.rookBlockerMask(curSquare);

        long bishopSubset = occupied & bishopMask;  // relevant blockers only
        long rookSusbet = occupied & rookMask;

        long bishopTargets = masks.getBishopMoves(curSquare, bishopSubset);
        long rookTargets = masks.getRookMoves(curSquare, rookSusbet);

        long targets = bishopTargets | rookTargets;

        // Remove own pieces
        targets &= ~own;

        Piece mover = isWhite ? Piece.WQ : Piece.BQ;
        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            targets &= targets - 1;

            Piece captured = ((enemy >>> to) & 1L) != 0 ? board.getPieceAtSquare(to) : null;
            list.add(new Move(curSquare, to, mover, captured, null, 0));
        }

        return list;
    }

    private static MoveList genRooks(Board board, int curSquare, boolean isWhite, Masks masks) {
        MoveList list = new MoveList();

        long own   = isWhite ? board.getAllWhitePieces() : board.getAllBlackPieces();
        long enemy = isWhite ? board.getAllBlackPieces() : board.getAllWhitePieces();

        long occupied = board.getAllPieces();

        long mask   = RookMoveMasks.rookBlockerMask(curSquare);
        long subset = occupied & mask;  // relevant blockers only

        long targets = masks.getRookMoves(curSquare, subset);

        // Remove own pieces
        targets &= ~own;

        Piece mover = isWhite ? Piece.WR : Piece.BR;
        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            targets &= targets - 1;

            Piece captured = ((enemy >>> to) & 1L) != 0 ? board.getPieceAtSquare(to) : null;
            list.add(new Move(curSquare, to, mover, captured, null, 0));
        }
        return list;
    }

    public static HashMap<Integer, MoveList> generatePseudoLegalMoves(Board board, Masks masks, boolean includeCastling){
        HashMap<Integer, MoveList> allMoves = new HashMap<>();

        long bb = board.getAllPieces();

        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);  // gets index of lowest 1-bit

            Piece curPiece = board.getPieceAtSquare(sq);

            if (curPiece == null) {continue;}

            MoveList curMoves = generate(board, sq, curPiece, masks, includeCastling);

            allMoves.put(sq, curMoves);

            bb &= bb - 1;  // removes the lowest 1-bit
        }

        return allMoves;
    }

    public static HashMap<Integer, MoveList> generatePseudoLegalMoves(Board board, Masks masks, Long bitboard, boolean includeCastling){
        HashMap<Integer, MoveList> allMoves = new HashMap<>();

        long bb = bitboard;

        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);  // gets index of lowest 1-bit

            Piece curPiece = board.getPieceAtSquare(sq);

            if (curPiece == null) {continue;}

            MoveList curMoves = generate(board, sq, curPiece, masks, includeCastling);

            allMoves.put(sq, curMoves);

            bb &= bb - 1;  // removes the lowest 1-bit
        }

        return allMoves;
    }


    public static HashMap<Integer, MoveList> generateLegalMoves(Board board, Masks masks){
        HashMap<Integer, MoveList> pseudoLegalMoves = generatePseudoLegalMoves(board, masks, true);
        HashMap<Integer, MoveList> legalMoves = new HashMap<>();

        for (int sq = 0; sq < 64; sq++){
            MoveList moveList = pseudoLegalMoves.get(sq);
            MoveList tempMoveList = new MoveList();

            if (moveList == null) { continue; }

            for (Move move : moveList){

                // do not allow moves that "capture" a king
                if (move.capture == Piece.WK || move.capture == Piece.BK) {
                    continue;
                }

                // 2) Test if the move leaves your own king in check
                board.makeMoveInternal(move);

                if (move.piece.isWhite()) {
                    if (!board.whiteInCheck) {
                        tempMoveList.add(move);
                    }
                }

                if (move.piece.isBlack()) {
                    if (!board.blackInCheck) {
                        tempMoveList.add(move);
                    }
                }

                board.unmakeMoveInternal(move);
            }

            legalMoves.put(sq, tempMoveList);
        }

        return legalMoves;
    }

}
