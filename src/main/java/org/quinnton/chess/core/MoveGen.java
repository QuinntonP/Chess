package org.quinnton.chess.core;

import java.util.ArrayList;

public final class MoveGen {

    public static MoveList generate(Board board, int curSquare, Piece firstPiece, Masks masks){
        MoveList list = new MoveList();

        // test
        Move testMove = new Move(curSquare, 30, firstPiece, Piece.BP, null, 0);
        list.add(testMove);

        list = switch (firstPiece) {
            case WP, BP -> genPawns(board, curSquare, firstPiece.white);
            case WN, BN -> genKnights(board, curSquare, firstPiece.white, masks);
            case WB, BB -> genBishops(board, curSquare, firstPiece.white);
            case WR, BR -> genRooks(board, curSquare, firstPiece.white, masks);
            case WQ, BQ -> genQueens(board, curSquare, firstPiece.white);
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

        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            targets &= targets - 1;

            Piece captured = ((enemy >>> to) & 1L) != 0 ? board.getPieceAtSquare(to) : null;
            list.add(new Move(curSquare, to, mover, captured, null, 0));
        }
        return list;
    }


    private static MoveList genPawns(Board board, int curSquare, boolean isWhite){
        MoveList list = new MoveList();

        return list;
    }

    private static MoveList genBishops(Board board, int curSquare, boolean isWhite){
        MoveList list = new MoveList();

        return list;
    }

    private static MoveList genQueens(Board board, int curSquare, boolean isWhite){
        MoveList list = new MoveList();

        return list;
    }

    private static MoveList genRooks(Board board, int curSquare, boolean isWhite, Masks masks){
        MoveList list = new MoveList();
        long curSquareMask = 1L << curSquare;
        int file = curSquare % 8;
        int rank = curSquare / 8;

        long R_MASK = ((Masks.RANKS[rank] | Masks.FILES[file]) & ~curSquareMask) & Masks.NOT_FILE_A & Masks.NOT_FILE_H;;

        masks.setMoveMask(Piece.WR, R_MASK);

        return list;
    }

}
