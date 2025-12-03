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
            legalMoves = board.getPseudoLegalMoves().get(sq);
            showHighlights();
            return;
        }

        Piece fromPiece = board.getPieceAtSquare(selectedFrom);

        if (clicked != null && fromPiece != null && clicked.white == fromPiece.white) {
            selectedFrom = sq;
            legalMoves = board.getPseudoLegalMoves().get(sq);
            showHighlights();
            return;
        }

        if (isLegalDestination(sq)) {
            board.makeMove(selectedFrom, sq);
            board.addTurnCounter();
            for (Move move : legalMoves){
                if (move.to == sq && selectedFrom == move.from){
                    board.setLastMove(move);
                }
            }
        }

        clearSelection();
    }

    private void showHighlights() {
        Set<Integer> squares = new HashSet<>();
        for (Move legalMove : legalMoves) squares.add(legalMove.to);
        squares.add(selectedFrom);
        view.setHighlights(squares);
    }

    private boolean isLegalDestination(int sq) {
        for (Move legalMove : legalMoves) if (legalMove.to == sq) return true;
        return false;
    }

    private void clearSelection() {
        selectedFrom = null;
        legalMoves = null;
        view.clearHighlights();
    }
}
