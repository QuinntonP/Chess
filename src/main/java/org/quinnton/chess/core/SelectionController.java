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

        if (selectedFrom == null) {
            if (clicked == null) return;
            selectedFrom = sq;
            legalMoves = MoveGen.generate(selectedFrom, clicked);
            showHighlights();
            return;
        }

        Piece fromPiece = board.getPieceAtSquare(selectedFrom);

        if (clicked != null && fromPiece != null && clicked.white == fromPiece.white) {
            selectedFrom = sq;
            legalMoves = MoveGen.generate(selectedFrom, clicked);
            showHighlights();
            return;
        }

        if (isLegalDestination(sq)) {
            board.makeMove(selectedFrom, sq);
        }

        clearSelection();
    }

    private void showHighlights() {
        Set<Integer> squares = new HashSet<>();
        for (int i = 0; i < legalMoves.size(); i++) squares.add(legalMoves.get(i).to);
        squares.add(selectedFrom);
        view.setHighlights(squares);
    }

    private boolean isLegalDestination(int sq) {
        for (int i = 0; i < legalMoves.size(); i++)
            if (legalMoves.get(i).to == sq) return true;
        return false;
    }

    private void clearSelection() {
        selectedFrom = null;
        legalMoves = null;
        view.clearHighlights();
    }
}
