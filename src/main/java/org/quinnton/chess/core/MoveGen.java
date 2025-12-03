package org.quinnton.chess.core;


import java.util.ArrayList;
import java.util.List;

public final class MoveGen {

    public static MoveList generate(Board board, int curSquare, Piece firstPiece, Masks masks){
        MoveList list = new MoveList();

        // test
        Move testMove = new Move(curSquare, 30, firstPiece, Piece.BP, null, 0);
        list.add(testMove);

        list = switch (firstPiece) {
            case WP, BP -> genPawns(board, curSquare, firstPiece.white);
            case WN, BN -> genKnights(board, curSquare, firstPiece.white, masks);
            case WB, BB -> genBishops(board, curSquare, firstPiece.white, masks);
            case WR, BR -> genRooks(board, curSquare, firstPiece.white, masks);
            case WQ, BQ -> genQueens(board, curSquare, firstPiece.white, masks);
            case WK, BK -> genKings(board, curSquare, firstPiece.white, masks);
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


    private static MoveList genKings(Board board, int curSquare, boolean isWhite, Masks masks) {
        MoveList list = new MoveList();
        long own = isWhite ? board.getAllWhitePieces() : board.getAllBlackPieces();
        long enemy = isWhite ? board.getAllBlackPieces() : board.getAllWhitePieces();

        long targets = masks.getKingMoves(curSquare) & ~own;
        Piece mover = isWhite ? Piece.WK : Piece.BK;

        // castling
        list = kingCastling(list, board, curSquare, isWhite);

        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            targets &= targets - 1;

            Piece captured = ((enemy >>> to) & 1L) != 0 ? board.getPieceAtSquare(to) : null;
            list.add(new Move(curSquare, to, mover, captured, null, 0));
        }
        return list;
    }


    private static MoveList kingCastling(MoveList moveList, Board board, int curSquare, boolean isWhite) {
        long occupied = board.getAllPieces();

        if (isWhite) {
            // King must be on starting square and unmoved
            if (board.whiteKingHasMoved || curSquare != 4) {
                return moveList;
            }

            // ---------- White king-side castling (e1 -> g1) ----------
            // Rook on h1 has not moved, squares f1 (5) and g1 (6) empty,
            // and e1, f1, g1 are not attacked by black.
            if (!board.whiteKingRookHasMoved) {
                boolean emptyF1 = (occupied & (1L << 5)) == 0;
                boolean emptyG1 = (occupied & (1L << 6)) == 0;

                if (emptyF1 && emptyG1
                        && !board.isSquareAttacked(4, false)  // e1
                        && !board.isSquareAttacked(5, false)  // f1
                        && !board.isSquareAttacked(6, false)  // g1
                ) {
                    moveList.add(new Move(
                            4, 6,
                            Piece.WK,
                            null,
                            null, // promotion / extra info if you use it
                            9999
                    ));
                }
            }

            // ---------- White queen-side castling (e1 -> c1) ----------
            // Rook on a1 has not moved, squares b1 (1), c1 (2), d1 (3) empty,
            // and e1, d1, c1 are not attacked by black.
            if (!board.whiteQueenRookHasMoved) {
                boolean emptyB1 = (occupied & (1L << 1)) == 0;
                boolean emptyC1 = (occupied & (1L << 2)) == 0;
                boolean emptyD1 = (occupied & (1L << 3)) == 0;

                if (emptyB1 && emptyC1 && emptyD1
                        && !board.isSquareAttacked(4, false)  // e1
                        && !board.isSquareAttacked(3, false)  // d1
                        && !board.isSquareAttacked(2, false)  // c1
                ) {
                    moveList.add(new Move(
                            4, 2,
                            Piece.WK,
                            null,
                            null,
                            9999
                    ));
                }
            }
        } else {
            // ---------- Black ----------
            // King must be on starting square and unmoved
            if (board.blackKingHasMoved || curSquare != 60) {
                return moveList;
            }

            // ---------- Black king-side castling (e8 -> g8) ----------
            // Rook on h8 has not moved, squares f8 (61) and g8 (62) empty,
            // and e8, f8, g8 are not attacked by white.
            if (!board.blackKingRookHasMoved) {
                boolean emptyF8 = (occupied & (1L << 61)) == 0;
                boolean emptyG8 = (occupied & (1L << 62)) == 0;

                if (emptyF8 && emptyG8
                        && !board.isSquareAttacked(60, true)  // e8
                        && !board.isSquareAttacked(61, true)  // f8
                        && !board.isSquareAttacked(62, true)  // g8
                ) {
                    moveList.add(new Move(
                            60, 62,
                            Piece.BK,
                            null,
                            null,
                            9999
                    ));
                }
            }

            // ---------- Black queen-side castling (e8 -> c8) ----------
            // Rook on a8 has not moved, squares b8 (57), c8 (58), d8 (59) empty,
            // and e8, d8, c8 are not attacked by white.
            if (!board.blackQueenRookHasMoved) {
                boolean emptyB8 = (occupied & (1L << 57)) == 0;
                boolean emptyC8 = (occupied & (1L << 58)) == 0;
                boolean emptyD8 = (occupied & (1L << 59)) == 0;

                if (emptyB8 && emptyC8 && emptyD8
                        && !board.isSquareAttacked(60, true)  // e8
                        && !board.isSquareAttacked(59, true)  // d8
                        && !board.isSquareAttacked(58, true)  // c8
                ) {
                    moveList.add(new Move(
                            60, 58,
                            Piece.BK,
                            null,
                            null,
                            9999
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
        long targets = 0L;

        Piece mover = isWhite ? Piece.WP : Piece.BP;

        int direction = 1;

        if(!isWhite){
            direction = -1;
        }

        // move 1 forward
        mask = 1L << (8 * direction + curSquare);
        if ((mask & occupied) == 0){
            targets |= mask;

            //  move forward 2
            mask = 1L << (16 * direction + curSquare);
            if ((mask & occupied) == 0 && (rank == 1 || rank == 6)){
                targets |= mask;
            }
        }

        // left capture
        mask = 1L << (7 * direction + curSquare);
        if ((mask & enemy) != 0){
            targets |= mask;
        }

        // right capture
        mask = 1L << (9 * direction + curSquare);
        if ((mask & enemy) != 0){
            targets |= mask;
        }

        // dang enpassant
        long enemyPawns = enemy & board.getAllPawns();

        Move myPreviousMove = isWhite ? board.getLastWhiteMove() : board.getLastBlackMove();

        if (((isWhite && rank == 4) || (!isWhite && rank == 3)) && myPreviousMove.to == curSquare){
            // right
            mask = 1L << curSquare + 1;
            if ((enemyPawns & mask) != 0){
                targets |= mask;
            }

            // left
            mask = 1L << curSquare -1;
            if ((enemyPawns & mask) != 0){
                targets |= mask;
            }

        }

        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            targets &= targets - 1;

            Piece captured = ((enemy >>> to) & 1L) != 0 ? board.getPieceAtSquare(to) : null;
            list.add(new Move(curSquare, to, mover, captured, null, 0));
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

        Piece mover = isWhite ? Piece.WR : Piece.BR;
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

        Piece mover = isWhite ? Piece.WR : Piece.BR;
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
}
