package org.quinnton.chess.core;

public final class MoveGen {

    public static MoveList generate(int curSquare, Piece firstPiece){
        MoveList list = new MoveList();

        // test
        Move testMove = new Move(curSquare, 30, firstPiece, Piece.BP, null, 0);
        list.add(testMove);

        return  list;
    }

    private static void genKnights(MoveList out){
        // Add things to the move list when called
    }

    private static void genKings(MoveList out){
        // Add things to the move list when called
    }

}
