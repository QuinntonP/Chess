package org.quinnton.chess.core;

import java.util.HashMap;

public final class MoveGen {
    private static final int MAX_MOVES = 256;

    // ---------------------------
    // Core entry: append moves into a buffer
    // ---------------------------
    public static void generateInto(
            Board board,
            int fromSq,
            Piece piece,
            Masks masks,
            boolean includeCastling,
            boolean pawnAttackMask,
            MoveBuffer out
    ) {
        switch (piece) {
            case WP, BP -> genPawns(board, fromSq, piece.isWhite(), pawnAttackMask, out);
            case WN, BN -> genKnights(board, fromSq, piece.isWhite(), masks, out);
            case WB, BB -> genBishops(board, fromSq, piece.isWhite(), masks, out);
            case WR, BR -> genRooks(board, fromSq, piece.isWhite(), masks, out);
            case WQ, BQ -> genQueens(board, fromSq, piece.isWhite(), masks, out);
            case WK, BK -> genKings(board, fromSq, piece.isWhite(), masks, includeCastling, out);
        }
    }

    // ---------------------------
    // Fast flat pseudo-legal generation (best for search)
    // ---------------------------
    public static MoveBuffer generatePseudoLegalMovesFlat(Board board, Masks masks, boolean includeCastling) {
        MoveBuffer out = new MoveBuffer(MAX_MOVES);
        out.clear();

        boolean whiteToMove = board.getTurnCounter();
        long bb = whiteToMove ? board.getAllWhitePieces() : board.getAllBlackPieces();

        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            bb &= bb - 1;

            Piece curPiece = board.getPieceAtSquare(sq);
            if (curPiece == null) continue;
            if (curPiece.isWhite() != whiteToMove) continue;

            generateInto(board, sq, curPiece, masks, includeCastling, false, out);
        }

        return out;
    }

    // ---------------------------
    // Flat LEGAL moves (filters by making/unmaking)
    // ---------------------------
    public static MoveBuffer generateLegalMovesFlat(Board board, Masks masks) {
        MoveBuffer pseudo = generatePseudoLegalMovesFlat(board, masks, true);
        MoveBuffer legal = new MoveBuffer(pseudo.size == 0 ? 1 : pseudo.size);

        boolean whiteToMove = board.getTurnCounter();

        for (int i = 0; i < pseudo.size; i++) {
            Move m = pseudo.moves[i];

            // Never allow “capturing” a king
            if (m.capture == Piece.WK || m.capture == Piece.BK) continue;
            if (m.piece.isWhite() != whiteToMove) continue;

            board.makeMoveInternal(m);
            boolean kingSafe = whiteToMove ? !board.whiteInCheck : !board.blackInCheck;
            board.unmakeMoveInternal(m);

            if (kingSafe) legal.add(m);
        }

        return legal;
    }

    // ---------------------------
    // If you still want the old map form (fromSq -> MoveList),
    // you can build it from a flat buffer (still avoids per-piece lists).
    // ---------------------------
    public static HashMap<Integer, MoveList> generatePseudoLegalMoves(Board board, Masks masks, boolean includeCastling) {
        HashMap<Integer, MoveList> allMoves = new HashMap<>();

        boolean whiteToMove = board.getTurnCounter();
        long bb = whiteToMove ? board.getAllWhitePieces() : board.getAllBlackPieces();

        // Reuse a buffer per piece to avoid allocating MoveList during generation
        MoveBuffer tmp = new MoveBuffer(MAX_MOVES);

        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            bb &= bb - 1;

            Piece curPiece = board.getPieceAtSquare(sq);
            if (curPiece == null) continue;
            if (curPiece.isWhite() != whiteToMove) continue;

            tmp.clear();
            generateInto(board, sq, curPiece, masks, includeCastling, false, tmp);

            if (tmp.size > 0) {
                MoveList list = new MoveList();
                for (int i = 0; i < tmp.size; i++) list.add(tmp.moves[i]);
                allMoves.put(sq, list);
            }
        }

        return allMoves;
    }

    public static HashMap<Integer, MoveList> generateLegalMoves(Board board, Masks masks) {
        HashMap<Integer, MoveList> pseudo = generatePseudoLegalMoves(board, masks, true);
        HashMap<Integer, MoveList> legal  = new HashMap<>();

        boolean whiteToMove = board.getTurnCounter();

        for (var entry : pseudo.entrySet()) {
            int fromSq = entry.getKey();
            MoveList srcList = entry.getValue();
            MoveList dstList = new MoveList();

            for (Move move : srcList) {
                if (move.capture == Piece.WK || move.capture == Piece.BK) continue;
                if (move.piece.isWhite() != whiteToMove) continue;

                board.makeMoveInternal(move);
                boolean kingSafe = whiteToMove ? !board.whiteInCheck : !board.blackInCheck;
                board.unmakeMoveInternal(move);

                if (kingSafe) dstList.add(move);
            }

            if (!dstList.isEmpty()) {
                legal.put(fromSq, dstList);
            }
        }

        return legal;
    }

    // ---------------------------
    // Piece generators (append into MoveBuffer)
    // ---------------------------

    private static void genKnights(Board board, int from, boolean isWhite, Masks masks, MoveBuffer out) {
        long own   = isWhite ? board.getAllWhitePieces() : board.getAllBlackPieces();
        long enemy = isWhite ? board.getAllBlackPieces() : board.getAllWhitePieces();

        long targets = masks.getKnightMoves(from) & ~own;
        Piece mover  = isWhite ? Piece.WN : Piece.BN;

        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            targets &= targets - 1;

            Piece captured = (((enemy >>> to) & 1L) != 0) ? board.getPieceAtSquare(to) : null;
            out.add(new Move(from, to, mover, captured, null, 0));
        }
    }

    private static void genKings(Board board, int from, boolean isWhite, Masks masks, boolean includeCastling, MoveBuffer out) {
        long own   = isWhite ? board.getAllWhitePieces() : board.getAllBlackPieces();
        long enemy = isWhite ? board.getAllBlackPieces() : board.getAllWhitePieces();

        long targets = masks.getKingMoves(from) & ~own;
        Piece mover  = isWhite ? Piece.WK : Piece.BK;

        if (includeCastling) {
            kingCastling(board, from, isWhite, out);
        }

        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            targets &= targets - 1;

            Piece captured = (((enemy >>> to) & 1L) != 0) ? board.getPieceAtSquare(to) : null;
            out.add(new Move(from, to, mover, captured, null, 0));
        }
    }

    private static void kingCastling(Board board, int curSquare, boolean isWhite, MoveBuffer out) {
        long occupied   = board.getAllPieces();
        long attackMask = board.getAttackMask(!isWhite); // attacked by opponent

        if (isWhite) {
            if (board.whiteKingHasMoved || curSquare != 4) return;

            // King-side (e1->g1)
            if (!board.whiteKingRookHasMoved && board.getPieceAtSquare(7) == Piece.WR) {
                boolean emptyF1 = (occupied & (1L << 5)) == 0;
                boolean emptyG1 = (occupied & (1L << 6)) == 0;

                boolean safeE1 = (attackMask & (1L << 4)) == 0;
                boolean safeF1 = (attackMask & (1L << 5)) == 0;
                boolean safeG1 = (attackMask & (1L << 6)) == 0;

                if (emptyF1 && emptyG1 && safeE1 && safeF1 && safeG1) {
                    out.add(new Move(4, 6, Piece.WK, null, null, 3));
                }
            }

            // Queen-side (e1->c1)
            if (!board.whiteQueenRookHasMoved && board.getPieceAtSquare(0) == Piece.WR) {
                boolean emptyB1 = (occupied & (1L << 1)) == 0;
                boolean emptyC1 = (occupied & (1L << 2)) == 0;
                boolean emptyD1 = (occupied & (1L << 3)) == 0;

                boolean safeE1 = (attackMask & (1L << 4)) == 0;
                boolean safeD1 = (attackMask & (1L << 3)) == 0;
                boolean safeC1 = (attackMask & (1L << 2)) == 0;

                if (emptyB1 && emptyC1 && emptyD1 && safeE1 && safeD1 && safeC1) {
                    out.add(new Move(4, 2, Piece.WK, null, null, 2));
                }
            }
        } else {
            if (board.blackKingHasMoved || curSquare != 60) return;

            // King-side (e8->g8)
            if (!board.blackKingRookHasMoved && board.getPieceAtSquare(63) == Piece.BR) {
                boolean emptyF8 = (occupied & (1L << 61)) == 0;
                boolean emptyG8 = (occupied & (1L << 62)) == 0;

                boolean safeE8 = (attackMask & (1L << 60)) == 0;
                boolean safeF8 = (attackMask & (1L << 61)) == 0;
                boolean safeG8 = (attackMask & (1L << 62)) == 0;

                if (emptyF8 && emptyG8 && safeE8 && safeF8 && safeG8) {
                    out.add(new Move(60, 62, Piece.BK, null, null, 3));
                }
            }

            // Queen-side (e8->c8)
            if (!board.blackQueenRookHasMoved && board.getPieceAtSquare(56) == Piece.BR) {
                boolean emptyB8 = (occupied & (1L << 57)) == 0;
                boolean emptyC8 = (occupied & (1L << 58)) == 0;
                boolean emptyD8 = (occupied & (1L << 59)) == 0;

                boolean safeE8 = (attackMask & (1L << 60)) == 0;
                boolean safeD8 = (attackMask & (1L << 59)) == 0;
                boolean safeC8 = (attackMask & (1L << 58)) == 0;

                if (emptyB8 && emptyC8 && emptyD8 && safeE8 && safeD8 && safeC8) {
                    out.add(new Move(60, 58, Piece.BK, null, null, 2));
                }
            }
        }
    }

    private static void genPawns(Board board, int from, boolean isWhite, boolean pawnAttackMask, MoveBuffer out) {
        int rank = from / 8;
        int file = from % 8;

        long occupied = board.getAllPieces();
        long enemyBB  = isWhite ? board.getAllBlackPieces() : board.getAllWhitePieces();
        Piece mover   = isWhite ? Piece.WP : Piece.BP;
        int dir       = isWhite ? 1 : -1;

        boolean promotionRank = (isWhite && rank == 6) || (!isWhite && rank == 1);

        // -------------------------
        // ATTACK MASK MODE:
        // Only diagonals, even if empty. No forward pushes, no EP, no promotions.
        // -------------------------
        if (pawnAttackMask) {
            if (isWhite) {
                if (file > 0) {
                    int to = from + 7;
                    if (to >= 0 && to < 64) out.add(new Move(from, to, mover, null, null, 0));
                }
                if (file < 7) {
                    int to = from + 9;
                    if (to >= 0 && to < 64) out.add(new Move(from, to, mover, null, null, 0));
                }
            } else {
                if (file > 0) {
                    int to = from - 9;
                    if (to >= 0 && to < 64) out.add(new Move(from, to, mover, null, null, 0));
                }
                if (file < 7) {
                    int to = from - 7;
                    if (to >= 0 && to < 64) out.add(new Move(from, to, mover, null, null, 0));
                }
            }
            return;
        }

        // -------------------------
        // NORMAL MOVE GEN
        // -------------------------

        // One step forward
        int fwd = from + 8 * dir;
        if (fwd >= 0 && fwd < 64 && ((occupied >>> fwd) & 1L) == 0) {
            if (promotionRank) {
                addPawnPromotions(from, fwd, mover, null, 1, out);
            } else {
                out.add(new Move(from, fwd, mover, null, null, 1));
            }

            // Two steps forward (only if one step was clear)
            boolean onStartRank = (isWhite && rank == 1) || (!isWhite && rank == 6);
            if (onStartRank) {
                int fwd2 = from + 16 * dir;
                if (fwd2 >= 0 && fwd2 < 64 && ((occupied >>> fwd2) & 1L) == 0) {
                    out.add(new Move(from, fwd2, mover, null, null, 1));
                }
            }
        }

        // Captures
        if (isWhite) {
            // NW: +7
            if (file > 0) {
                int to = from + 7;
                if (to >= 0 && to < 64 && ((enemyBB >>> to) & 1L) != 0) {
                    Piece cap = board.getPieceAtSquare(to);
                    if (promotionRank) addPawnPromotions(from, to, mover, cap, 0, out);
                    else out.add(new Move(from, to, mover, cap, null, 0));
                }
            }
            // NE: +9
            if (file < 7) {
                int to = from + 9;
                if (to >= 0 && to < 64 && ((enemyBB >>> to) & 1L) != 0) {
                    Piece cap = board.getPieceAtSquare(to);
                    if (promotionRank) addPawnPromotions(from, to, mover, cap, 0, out);
                    else out.add(new Move(from, to, mover, cap, null, 0));
                }
            }
        } else {
            // SW: -9
            if (file > 0) {
                int to = from - 9;
                if (to >= 0 && to < 64 && ((enemyBB >>> to) & 1L) != 0) {
                    Piece cap = board.getPieceAtSquare(to);
                    if (promotionRank) addPawnPromotions(from, to, mover, cap, 0, out);
                    else out.add(new Move(from, to, mover, cap, null, 0));
                }
            }
            // SE: -7
            if (file < 7) {
                int to = from - 7;
                if (to >= 0 && to < 64 && ((enemyBB >>> to) & 1L) != 0) {
                    Piece cap = board.getPieceAtSquare(to);
                    if (promotionRank) addPawnPromotions(from, to, mover, cap, 0, out);
                    else out.add(new Move(from, to, mover, cap, null, 0));
                }
            }
        }

        // En passant
        int ep = board.getEnPassantSquare();
        if (ep != -1) {
            if (isWhite) {
                if (file > 0 && from + 7 == ep) out.add(new Move(from, ep, mover, null, null, 4));
                if (file < 7 && from + 9 == ep) out.add(new Move(from, ep, mover, null, null, 4));
            } else {
                if (file > 0 && from - 9 == ep) out.add(new Move(from, ep, mover, null, null, 4));
                if (file < 7 && from - 7 == ep) out.add(new Move(from, ep, mover, null, null, 4));
            }
        }
    }


    private static void addPawnPromotions(int from, int to, Piece pawn, Piece capture, int flags, MoveBuffer out) {
        Piece[] promos = pawn.isWhite()
                ? new Piece[]{Piece.WQ, Piece.WR, Piece.WB, Piece.WN}
                : new Piece[]{Piece.BQ, Piece.BR, Piece.BB, Piece.BN};

        for (Piece promo : promos) {
            out.add(new Move(from, to, pawn, capture, promo, flags));
        }
    }

    private static void genBishops(Board board, int from, boolean isWhite, Masks masks, MoveBuffer out) {
        long own   = isWhite ? board.getAllWhitePieces() : board.getAllBlackPieces();
        long enemy = isWhite ? board.getAllBlackPieces() : board.getAllWhitePieces();
        long occ   = board.getAllPieces();

        long mask   = BishopMoveMasks.bishopBlockerMask(from);
        long subset = occ & mask;

        long targets = masks.getBishopMoves(from, subset) & ~own;
        Piece mover  = isWhite ? Piece.WB : Piece.BB;

        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            targets &= targets - 1;

            Piece captured = (((enemy >>> to) & 1L) != 0) ? board.getPieceAtSquare(to) : null;
            out.add(new Move(from, to, mover, captured, null, 0));
        }
    }

    private static void genRooks(Board board, int from, boolean isWhite, Masks masks, MoveBuffer out) {
        long own   = isWhite ? board.getAllWhitePieces() : board.getAllBlackPieces();
        long enemy = isWhite ? board.getAllBlackPieces() : board.getAllWhitePieces();
        long occ   = board.getAllPieces();

        long mask   = RookMoveMasks.rookBlockerMask(from);
        long subset = occ & mask;

        long targets = masks.getRookMoves(from, subset) & ~own;
        Piece mover  = isWhite ? Piece.WR : Piece.BR;

        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            targets &= targets - 1;

            Piece captured = (((enemy >>> to) & 1L) != 0) ? board.getPieceAtSquare(to) : null;
            out.add(new Move(from, to, mover, captured, null, 0));
        }
    }

    private static void genQueens(Board board, int from, boolean isWhite, Masks masks, MoveBuffer out) {
        long own   = isWhite ? board.getAllWhitePieces() : board.getAllBlackPieces();
        long enemy = isWhite ? board.getAllBlackPieces() : board.getAllWhitePieces();
        long occ   = board.getAllPieces();

        long bMask = BishopMoveMasks.bishopBlockerMask(from);
        long rMask = RookMoveMasks.rookBlockerMask(from);

        long bSubset = occ & bMask;
        long rSubset = occ & rMask;

        long targets = (masks.getBishopMoves(from, bSubset) | masks.getRookMoves(from, rSubset)) & ~own;
        Piece mover  = isWhite ? Piece.WQ : Piece.BQ;

        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            targets &= targets - 1;

            Piece captured = (((enemy >>> to) & 1L) != 0) ? board.getPieceAtSquare(to) : null;
            out.add(new Move(from, to, mover, captured, null, 0));
        }
    }
}
