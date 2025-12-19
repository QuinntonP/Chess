package org.quinnton.chess.core;

import org.quinnton.chess.bot.Bot;

import java.util.HashSet;
import java.util.Set;

public class SelectionController {

    private final Board board;
    private final BoardView view;
    private final Bot bot;

    private Integer selectedFrom = null;

    // Promotion UI state
    private int pendingPromotionBaseMove = 0; // encoded move (one of the promo moves; we’ll swap promo piece later)
    private boolean decidingPromotion = false;

    public SelectionController(Board board, BoardView view, Bot bot) {
        this.board = board;
        this.view = view;
        this.bot = bot;
    }

    private void tryBotMove() {
        if (board.gameOver) return;

        boolean botPlaysWhite = false; // example
        boolean whiteToMove = board.getTurnCounter();
        boolean botToMove = (whiteToMove == botPlaysWhite);

        if (!botToMove) return;

        new Thread(() -> {
            Board searchBoard = board.copy();
            int best = bot.findBestMove(searchBoard, 4); // now returns encoded int
            if (best == 0) return;

            javafx.application.Platform.runLater(() -> {
                board.makeMove(best);
                board.addTurnCounter();
                board.setLastMove(best);
                board.lookForCheckmate();
                clearSelection();
            });
        }, "Bot-Search-Thread").start();
    }

    public void onSquareClick(int sq) {

        // ------------------------------------------------------------
        // Promotion choice mode
        // ------------------------------------------------------------
        if (decidingPromotion) {
            boolean isWhite = board.getTurnCounter();
            Piece choice = promoChoiceFromSquare(sq, isWhite);

            if (choice != null && pendingPromotionBaseMove != 0) {
                int from = Move.from(pendingPromotionBaseMove);
                int to = Move.to(pendingPromotionBaseMove);
                int flags = Move.flags(pendingPromotionBaseMove);

                Piece pawn = Move.piece(pendingPromotionBaseMove);
                Piece cap = Move.capture(pendingPromotionBaseMove);

                int finalMove = Move.pack(from, to, pawn, cap, choice, flags);

                board.makeMove(finalMove);
                board.addTurnCounter();
                board.setLastMove(finalMove);
                board.lookForCheckmate();
            }

            decidingPromotion = false;
            pendingPromotionBaseMove = 0;
            view.setDrawPawnPromotion(false);
            clearSelection();
            return;
        }

        // ------------------------------------------------------------
        // Normal click logic
        // ------------------------------------------------------------
        Piece clicked = board.getPieceAtSquare(sq);
        boolean whiteToMove = board.getTurnCounter();

        // If nothing selected, you can only select your own piece
        if (selectedFrom == null) {
            if (clicked == null) return;
            if (clicked.white != whiteToMove) {
                clearSelection();
                return;
            }

            selectedFrom = sq;
            showHighlights();
            return;
        }

        // If you clicked another friendly piece, switch selection
        Piece fromPiece = board.getPieceAtSquare(selectedFrom);
        if (clicked != null && fromPiece != null && clicked.white == fromPiece.white) {
            selectedFrom = sq;
            showHighlights();
            return;
        }

        // Try to find a move from selectedFrom -> sq
        int move = findMove(selectedFrom, fromPiece, sq);
        if (move == 0) {
            clearSelection();
            return;
        }

        // Promotion: if there are promo moves for this destination, open the promo UI
        if ((fromPiece == Piece.WP || fromPiece == Piece.BP) && Move.promoId(move) != 0) {
            // store one of the promotion moves as the “base” will swap promo later
            pendingPromotionBaseMove = move;
            decidingPromotion = true;
            view.setDrawPawnPromotion(true);
            return;
        }

        // Normal move
        board.makeMove(move);
        board.addTurnCounter();
        board.setLastMove(move);
        board.lookForCheckmate();
        clearSelection();

        tryBotMove();
    }

    private void showHighlights() {
        Set<Integer> squares = new HashSet<>();

        if (selectedFrom != null) {
            int[] moves = board.getLegalMovesArray();
            int count = board.getLegalMoveCount();

            for (int i = 0; i < count; i++) {
                int m = moves[i];
                if (Move.from(m) == selectedFrom) {
                    squares.add(Move.to(m));
                }
            }

            squares.add(selectedFrom);
        }

        view.setHighlights(squares);
    }

    private void clearSelection() {
        selectedFrom = null;
        view.clearHighlights();
    }

    /**
     * Finds a move in the board’s flat legal move list that matches:
     * from == selectedFrom AND to == dest AND mover piece matches.
     *
     * Returns 0 if not found.
     */
    private int findMove(int from, Piece piece, int dest) {
        if (piece == null) return 0;

        int[] moves = board.getLegalMovesArray();
        int count = board.getLegalMoveCount();

        for (int i = 0; i < count; i++) {
            int m = moves[i];
            if (Move.from(m) == from && Move.to(m) == dest && Move.piece(m) == piece) {
                return m; // first match is fine (promotion will be handled by UI)
            }
        }
        return 0;
    }

    private Piece promoChoiceFromSquare(int sq, boolean isWhite) {
        int file = sq % 8; // 0..7
        int rank = sq / 8; // 0..7 (a1=0)

        // window covers files 3..4 and ranks 3..4 (because xPos=yPos=sqSize*3)
        if (file < 3 || file > 4 || rank < 3 || rank > 4) return null;

        int x = file - 3; // 0..1
        int y = rank - 3; // 0..1
        int idx = x + y * 2;

        Piece[] pieces = isWhite
                ? new Piece[]{Piece.WB, Piece.WN, Piece.WQ, Piece.WR}
                : new Piece[]{Piece.BB, Piece.BN, Piece.BQ, Piece.BR};

        return pieces[idx];
    }
}
