package org.quinnton.chess.core;

import java.util.HashSet;
import java.util.Set;

public class SelectionController {
    private final Board board;
    private final BoardView view;

    private Integer selectedFrom = null;
    private MoveList legalMoves = null;


    public SelectionController(Board board, BoardView view) {
        this.board = board;
        this.view = view;
    }


    public void onSquareClick(int sq) {
        Piece clicked = board.getPieceAtSquare(sq);

        // Only click if it is that pieces turn
        if (selectedFrom == null && clicked != null && clicked.white != board.getTurnCounter()){
            selectedFrom = null;
            clearSelection();
            return;
        }

        if (selectedFrom == null) {
            if (clicked == null) return;
            selectedFrom = sq;
            // this guy
            legalMoves = board.getLegalMoves().get(sq);
            showHighlights();
            return;
        }

        Piece fromPiece = board.getPieceAtSquare(selectedFrom);

        if (clicked != null && fromPiece != null && clicked.white == fromPiece.white) {
            selectedFrom = sq;
            // and this guy
            legalMoves = board.getLegalMoves().get(sq);
            showHighlights();
            return;
        }

        Move move = getMoveFromMoves(legalMoves, fromPiece, sq);

        if (move != null) {
            board.makeMove(move);
            board.addTurnCounter();
            board.setLastMove(move);
            board.lookForCheckmate();
        }

        clearSelection();
    }


    private void showHighlights() {
        Set<Integer> squares = new HashSet<>();
        for (Move legalMove : legalMoves) squares.add(legalMove.to);
        squares.add(selectedFrom);
        view.setHighlights(squares);
    }


    private void clearSelection() {
        selectedFrom = null;
        legalMoves = null;
        view.clearHighlights();
    }

    /**
     *
     * @param moveList the list of moves
     * @param piece the pice type
     * @param dest the destination square of the move
     * @return the move that fits the all the criteria
     */
    private Move getMoveFromMoves(MoveList moveList, Piece piece, int dest){
        for (Move move : moveList){
            if (move.to == dest && move.piece == piece){
                return move;
            }
        }
        return null;
    }
}
