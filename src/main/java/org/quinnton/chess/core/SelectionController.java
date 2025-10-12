package org.quinnton.chess.core;

import java.util.HashSet;
import java.util.Set;

public class SelectionController {
    private final Board board;
    private final BoardView view;

    private final Masks masks;

    private Integer selectedFrom = null;
    private MoveList legalMoves = null;



    public SelectionController(Board board, BoardView view, Masks masks) {
        this.board = board;
        this.view = view;
        this.masks = masks;
    }

    public void onSquareClick(int sq) {
        Piece clicked = board.getPieceAtSquare(sq);

        if (selectedFrom == null) {
            if (clicked == null) return;
            selectedFrom = sq;
            legalMoves = MoveGen.generate(board, selectedFrom, clicked, masks);
            showHighlights();
            return;
        }

        Piece fromPiece = board.getPieceAtSquare(selectedFrom);

        if (clicked != null && fromPiece != null && clicked.white == fromPiece.white) {
            selectedFrom = sq;
            legalMoves = MoveGen.generate(board, selectedFrom, clicked, masks);
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
