package org.quinnton.chess.core;

/**
 * The single place that advances a game by one ply.
 *
 * Applying a move is more than {@link Board#makeMove(int)}: the turn must be
 * advanced (which regenerates legal moves and refreshes check flags), the move
 * recorded, and the terminal conditions re-evaluated in the right order
 * (checkmate/stalemate first, then the automatic draws). UI code
 * ({@code MoveDecider}) and the headless runner both need exactly this
 * sequence, so it lives here once rather than being duplicated and drifting.
 */
public final class GameRules {

    private GameRules() {}

    /**
     * Apply an already-legal encoded move to {@code board} and update all game
     * state: turn ownership, last-move bookkeeping, and game-over/winner/draw
     * detection. After this returns, {@link Board#gameOver},
     * {@link Board#getWinnerIsWhite()} and {@link Board#getEndReason()} reflect
     * the new position.
     *
     * @param board the live game board (mutated in place)
     * @param move  the encoded move to apply
     */
    public static void applyMove(Board board, int move) {
        board.makeMove(move);
        board.addTurnCounter();
        board.setLastMove(move);
        board.lookForCheckmate();
        board.lookForDraw(move);
    }
}
