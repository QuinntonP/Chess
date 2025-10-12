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
            case WR, BR -> genRooks(board, curSquare, firstPiece.white);
            case WQ, BQ -> genQueens(board, curSquare, firstPiece.white);
            case WK, BK -> genKings(board, curSquare, firstPiece.white);
        };

        return  list;
    }

    private static MoveList genKnights(Board board, int curSquare, boolean isWhite, Masks masks){
        MoveList list = new MoveList();

        ArrayList<Integer> squares = Utils.extractSquares(masks.getKnightMoves(curSquare));

        Piece curPiece = isWhite ? Piece.WN : Piece.BN;

        for (int i : squares){
            Piece capturePiece = board.getPieceAtSquare(i);
            list.add(new Move(curSquare, i, curPiece, capturePiece, null, 0));
        }

        return list;
    }

    private static MoveList genKings(Board board, int curSquare, boolean isWhite){
        MoveList list = new MoveList();

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

    private static MoveList genRooks(Board board, int curSquare, boolean isWhite){
        MoveList list = new MoveList();

        return list;
    }

}
