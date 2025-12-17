package org.quinnton.chess.core;

import org.quinnton.chess.bot.Bot;

import java.util.HashSet;
import java.util.Set;

public class SelectionController {
    private final Board board;
    private final BoardView view;
    private final Bot bot;

    private Integer selectedFrom = null;
    private MoveList legalMoves = null;

    private Move pendingPromotionBaseMove = null; // same from/to/capture but promo unset
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
            Board serachBoard = board.copy();
             Move best = bot.findBestMove(serachBoard, 4);

            // debug version
//            Move best = bot.findBestMove(board, 5);
            if (best == null) return;

            // ✅ apply move ON the FX thread
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
        if (decidingPromotion) {
            boolean isWhite = board.getTurnCounter();
            Piece choice = promoChoiceFromSquare(sq, isWhite);

            if (choice != null && pendingPromotionBaseMove != null) {
                Move finalMove = new Move(
                        pendingPromotionBaseMove.from,
                        pendingPromotionBaseMove.to,
                        pendingPromotionBaseMove.piece,
                        pendingPromotionBaseMove.capture,
                        choice,
                        pendingPromotionBaseMove.flags
                );

                board.makeMove(finalMove);
                board.addTurnCounter();
                board.setLastMove(finalMove);
                board.lookForCheckmate();
            }

            decidingPromotion = false;
            pendingPromotionBaseMove = null;
            view.setDrawPawnPromotion(false);
            clearSelection();
            return;
        }

        Piece clicked = board.getPieceAtSquare(sq);

        if (selectedFrom == null && clicked != null && clicked.white != board.getTurnCounter()) {
            clearSelection();
            return;
        }

        if (selectedFrom == null) {
            if (clicked == null) return;
            selectedFrom = sq;
            legalMoves = board.getLegalMoves().get(sq);
            showHighlights();
            return;
        }

        Piece fromPiece = board.getPieceAtSquare(selectedFrom);

        if (clicked != null && fromPiece != null && clicked.white == fromPiece.white) {
            selectedFrom = sq;
            legalMoves = board.getLegalMoves().get(sq);
            showHighlights();
            return;
        }

        Move move = getMoveFromMoves(legalMoves, fromPiece, sq);
        if (move == null) {
            clearSelection();
            return;
        }

        // If destination has multiple promo moves, enter "choose piece" mode.
        if ((fromPiece == Piece.WP || fromPiece == Piece.BP) && move.promo != null) {
            // store base move without promo
            pendingPromotionBaseMove = new Move(move.from, move.to, move.piece, move.capture, null, move.flags);
            decidingPromotion = true;
            view.setDrawPawnPromotion(true);
            return;
        }

        board.makeMove(move);
        board.addTurnCounter();
        board.setLastMove(move);
        board.lookForCheckmate();
        clearSelection();

        tryBotMove();
    }



    private void showHighlights() {
        Set<Integer> squares = new HashSet<>();
        if (legalMoves != null){
            for (Move legalMove : legalMoves) squares.add(legalMove.to);
        }
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

    private Piece promoChoiceFromSquare(int sq, boolean isWhite) {
        int file = sq % 8;     // 0..7
        int rank = sq / 8;     // 0..7 (a1=0)

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
