package org.quinnton.chess.bot;

import org.quinnton.chess.core.Board;
import org.quinnton.chess.core.Move;
import org.quinnton.chess.core.Piece;

/**
 * Incremental evaluator: material + PST (piece-square tables).
 * Scores are from White's POV (positive = better for White).
 */
public class Evaluate {
    private final Board board;

    private int materialScore;
    private int pstScore;

    public Evaluate(Board board) {
        this.board = board;
        rebuildEvalFromScratch();
    }

    public int score() {
        return materialScore + pstScore;
    }

    /** Recompute eval from the board bitboards (use after loading FEN / new game). */
    public void rebuildEvalFromScratch() {
        materialScore = computeMaterial();
        pstScore = computePST();
    }

    /**
     * Apply eval deltas for a move. Call this exactly once per makeMoveInternal(m),
     * and store the returned EvalUndo so unmake can reverse it.
     */
    public EvalUndo updateMakeMove(int m) {
        int matDelta = 0;
        int pstDelta = 0;

        final int from = Move.from(m);
        final int to = Move.to(m);
        final int flags = Move.flags(m);

        final Piece mover = Move.piece(m);
        final Piece promo = Move.promo(m);

        // 1) mover PST: remove from 'from', add at 'to'
        // NOTE: for promotions we still treat "mover" as the pawn here; we fix PST below.
        pstDelta += pst(mover, to) - pst(mover, from);

        // 2) captures (normal or en-passant)
        // For normal captures, capId will be non-zero.
        // For EP, capId might be 0, so we handle EP explicitly.
        if (Move.capId(m) != 0 || flags == Move.FLAG_EN_PASSANT) {
            int capSq = to;
            Piece captured;

            if (flags == Move.FLAG_EN_PASSANT) {
                // EP capture square is behind destination
                capSq = mover.white ? (to - 8) : (to + 8);
                captured = mover.white ? Piece.BP : Piece.WP;
            } else {
                captured = Move.capture(m);
            }

            // captured piece is removed from board
            matDelta -= value(captured);
            pstDelta -= pst(captured, capSq);
        }

        // 3) promotion: pawn becomes promoted piece on 'to'
        if (promo != null) {
            // material: remove pawn, add promoted piece
            matDelta += value(promo) - value(mover); // mover should be WP/BP here

            // PST: replace pawn-at-to with promo-at-to
            pstDelta -= pst(mover, to);   // remove pawn-at-to we just added
            pstDelta += pst(promo, to);   // add promoted piece at to
        }

        // 4) castling: rook moves too (PST only)
        if (flags == Move.FLAG_CASTLE_QS || flags == Move.FLAG_CASTLE_KS) {
            int rookFrom, rookTo;
            boolean white = mover.white;

            if (flags == Move.FLAG_CASTLE_KS) { // king-side
                // e1->g1 rook h1->f1 ; e8->g8 rook h8->f8
                rookFrom = white ? 7 : 63;
                rookTo   = white ? 5 : 61;
            } else { // queen-side
                // e1->c1 rook a1->d1 ; e8->c8 rook a8->d8
                rookFrom = white ? 0 : 56;
                rookTo   = white ? 3 : 59;
            }

            Piece rook = white ? Piece.WR : Piece.BR;
            pstDelta += pst(rook, rookTo) - pst(rook, rookFrom);
        }

        materialScore += matDelta;
        pstScore += pstDelta;

        return new EvalUndo(matDelta, pstDelta);
    }

    /**
     * Reverse eval deltas for unmakeMoveInternal(m).
     * Pass the EvalUndo you got from updateMakeMove(m).
     */
    public void updateUnmakeMove(EvalUndo u) {
        materialScore -= u.matDelta;
        pstScore -= u.pstDelta;
    }

    /** Stores the deltas applied for a move so unmake can reverse them safely. */
    public static final class EvalUndo {
        public final int matDelta;
        public final int pstDelta;

        public EvalUndo(int matDelta, int pstDelta) {
            this.matDelta = matDelta;
            this.pstDelta = pstDelta;
        }
    }

    // -------------------------
    // Material + PST helpers
    // -------------------------

    private static int value(Piece p) {
        int v = switch (p) {
            case WP, BP -> 100;
            case WN, BN -> 300;
            case WB, BB -> 300;
            case WR, BR -> 500;
            case WQ, BQ -> 900;
            case WK, BK -> 0;
        };
        return p.white ? v : -v;
    }

    private static int mirror(int sq) {
        return sq ^ 56; // rank flip for a1=0..h8=63 indexing
    }

    private int pst(Piece p, int sq) {
        int s = p.white ? sq : mirror(sq);
        int v = switch (p) {
            case WP, BP -> PAWN_PST[s];
            case WN, BN -> KNIGHT_PST[s];
            case WB, BB -> BISHOP_PST[s];
            case WR, BR -> ROOK_PST[s];
            case WQ, BQ -> QUEEN_PST[s];
            case WK, BK -> KING_MG_PST[s]; // later: blend MG/EG
        };
        return p.white ? v : -v;
    }

    private int computeMaterial() {
        int s = 0;

        s += 100 * Long.bitCount(board.getBitboard(Piece.WP));
        s += 300 * Long.bitCount(board.getBitboard(Piece.WN));
        s += 300 * Long.bitCount(board.getBitboard(Piece.WB));
        s += 500 * Long.bitCount(board.getBitboard(Piece.WR));
        s += 900 * Long.bitCount(board.getBitboard(Piece.WQ));

        s -= 100 * Long.bitCount(board.getBitboard(Piece.BP));
        s -= 300 * Long.bitCount(board.getBitboard(Piece.BN));
        s -= 300 * Long.bitCount(board.getBitboard(Piece.BB));
        s -= 500 * Long.bitCount(board.getBitboard(Piece.BR));
        s -= 900 * Long.bitCount(board.getBitboard(Piece.BQ));

        return s;
    }

    private int computePST() {
        int s = 0;

        s += pstSum(Piece.WP, board.getBitboard(Piece.WP));
        s += pstSum(Piece.WN, board.getBitboard(Piece.WN));
        s += pstSum(Piece.WB, board.getBitboard(Piece.WB));
        s += pstSum(Piece.WR, board.getBitboard(Piece.WR));
        s += pstSum(Piece.WQ, board.getBitboard(Piece.WQ));
        s += pstSum(Piece.WK, board.getBitboard(Piece.WK));

        s += pstSum(Piece.BP, board.getBitboard(Piece.BP));
        s += pstSum(Piece.BN, board.getBitboard(Piece.BN));
        s += pstSum(Piece.BB, board.getBitboard(Piece.BB));
        s += pstSum(Piece.BR, board.getBitboard(Piece.BR));
        s += pstSum(Piece.BQ, board.getBitboard(Piece.BQ));
        s += pstSum(Piece.BK, board.getBitboard(Piece.BK));

        return s;
    }

    private int pstSum(Piece p, long bb) {
        int sum = 0;
        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            sum += pst(p, sq);
            bb &= bb - 1;
        }
        return sum;
    }

    // -------------------------
    // PST tables (centipawns)
    // -------------------------

    static final int[] PAWN_PST = {
            0,  0,  0,  0,  0,  0,  0,  0,
            5, 10, 10,-20,-20, 10, 10,  5,
            5, -5,-10,  0,  0,-10, -5,  5,
            0,  0,  0, 20, 20,  0,  0,  0,
            5,  5, 10, 25, 25, 10,  5,  5,
            10, 10, 20, 30, 30, 20, 10, 10,
            50, 50, 50, 50, 50, 50, 50, 50,
            0,  0,  0,  0,  0,  0,  0,  0
    };

    static final int[] KNIGHT_PST = {
            -50,-40,-30,-30,-30,-30,-40,-50,
            -40,-20,  0,  0,  0,  0,-20,-40,
            -30,  0, 10, 15, 15, 10,  0,-30,
            -30,  5, 15, 20, 20, 15,  5,-30,
            -30,  0, 15, 20, 20, 15,  0,-30,
            -30,  5, 10, 15, 15, 10,  5,-30,
            -40,-20,  0,  5,  5,  0,-20,-40,
            -50,-40,-30,-30,-30,-30,-40,-50
    };

    static final int[] BISHOP_PST = {
            -20,-10,-10,-10,-10,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5, 10, 10,  5,  0,-10,
            -10,  5,  5, 10, 10,  5,  5,-10,
            -10,  0, 10, 10, 10, 10,  0,-10,
            -10, 10, 10, 10, 10, 10, 10,-10,
            -10,  5,  0,  0,  0,  0,  5,-10,
            -20,-10,-10,-10,-10,-10,-10,-20
    };

    static final int[] ROOK_PST = {
            0,  0,  5, 10, 10,  5,  0,  0,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            5, 10, 10, 10, 10, 10, 10,  5,
            0,  0,  0,  0,  0,  0,  0,  0
    };

    static final int[] QUEEN_PST = {
            -20,-10,-10, -5, -5,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5,  5,  5,  5,  0,-10,
            -5,  0,  5,  5,  5,  5,  0, -5,
            0,  0,  5,  5,  5,  5,  0, -5,
            -10,  5,  5,  5,  5,  5,  0,-10,
            -10,  0,  5,  0,  0,  0,  0,-10,
            -20,-10,-10, -5, -5,-10,-10,-20
    };

    static final int[] KING_MG_PST = {
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -20,-30,-30,-40,-40,-30,-30,-20,
            -10,-20,-20,-20,-20,-20,-20,-10,
            20, 20,  0,  0,  0,  0, 20, 20,
            20, 30, 10,  0,  0, 10, 30, 20
    };

    static final int[] KING_EG_PST = {
            -50,-40,-30,-20,-20,-30,-40,-50,
            -30,-20,-10,  0,  0,-10,-20,-30,
            -30,-10, 20, 30, 30, 20,-10,-30,
            -30,-10, 30, 40, 40, 30,-10,-30,
            -30,-10, 30, 40, 40, 30,-10,-30,
            -30,-10, 20, 30, 30, 20,-10,-30,
            -30,-30,  0,  0,  0,  0,-30,-30,
            -50,-30,-30,-30,-30,-30,-30,-50
    };
}
