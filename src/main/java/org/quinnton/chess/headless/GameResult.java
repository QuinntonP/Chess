package org.quinnton.chess.headless;

import org.quinnton.chess.core.Board;

import java.util.List;

/**
 * The outcome of a single headless game.
 *
 * @param winnerIsWhite TRUE if white won, FALSE if black won, null on any draw
 * @param reason        how the game ended
 * @param plies         number of half-moves actually played
 * @param whiteSeed     seed of the white bot (for reproducing the game)
 * @param blackSeed     seed of the black bot
 * @param whiteNodes    total search nodes the white bot visited across the game
 * @param blackNodes    total search nodes the black bot visited across the game
 * @param uciMoves      the moves played, in UCI long-algebraic form (e2e4, ...)
 * @param finishedNaturally false if the game stopped on the ply cap or a bot
 *                          returned no move rather than a real terminal state
 */
public record GameResult(
        Boolean winnerIsWhite,
        Board.EndReason reason,
        int plies,
        long whiteSeed,
        long blackSeed,
        long whiteNodes,
        long blackNodes,
        List<String> uciMoves,
        boolean finishedNaturally
) {
    /** @return "1-0", "0-1", or "1/2-1/2". */
    public String scoreString() {
        if (winnerIsWhite == null) return "1/2-1/2";
        return winnerIsWhite ? "1-0" : "0-1";
    }
}
