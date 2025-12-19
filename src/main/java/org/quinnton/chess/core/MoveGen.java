package org.quinnton.chess.core;

public final class MoveGen {
    private static final int MAX_MOVES = 256;

    private MoveGen() {}

    // ------------------------------------------------------------
    // Core entry: append moves into `out`, return updated count
    // ------------------------------------------------------------
    public static int generateInto(
            Board board,
            int fromSq,
            Piece piece,
            Masks masks,
            boolean includeCastling,
            boolean pawnAttackMask,
            int[] out,
            int count
    ) {
        return switch (piece) {
            case WP, BP -> genPawns(board, fromSq, piece.isWhite(), pawnAttackMask, out, count);
            case WN, BN -> genKnights(board, fromSq, piece.isWhite(), masks, out, count);
            case WB, BB -> genBishops(board, fromSq, piece.isWhite(), masks, out, count);
            case WR, BR -> genRooks(board, fromSq, piece.isWhite(), masks, out, count);
            case WQ, BQ -> genQueens(board, fromSq, piece.isWhite(), masks, out, count);
            case WK, BK -> genKings(board, fromSq, piece.isWhite(), masks, includeCastling, out, count);
        };
    }

    // ------------------------------------------------------------
    // Flat pseudo-legal generation (best for search)
    // Caller provides `out` and receives count
    // ------------------------------------------------------------
    public static int generatePseudoLegalMovesFlat(
            Board board,
            Masks masks,
            boolean includeCastling,
            int[] out
    ) {
        int count = 0;

        boolean whiteToMove = board.getTurnCounter();
        long bb = whiteToMove ? board.getAllWhitePieces() : board.getAllBlackPieces();

        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            bb &= bb - 1;

            Piece piece = board.getPieceAtSquare(sq);
            if (piece == null) continue;
            if (piece.isWhite() != whiteToMove) continue;

            count = generateInto(board, sq, piece, masks, includeCastling, false, out, count);

            // If you ever overflow here, your out[] is too small.
            // MAX_MOVES=256 should be safe for pseudo-legal.
            if (count >= out.length) return out.length;
        }

        return count;
    }

    // ------------------------------------------------------------
    // Flat LEGAL generation: filters pseudo by make/unmake
    // Writes legal moves into `out`, returns count
    // Uses a local temp array to hold pseudo moves.
    // ------------------------------------------------------------
    public static int generateLegalMovesFlat(Board board, Masks masks, int[] out) {
        // temp pseudo buffer
        int[] pseudo = new int[MAX_MOVES];
        int pseudoCount = generatePseudoLegalMovesFlat(board, masks, true, pseudo);

        int count = 0;
        boolean whiteToMove = board.getTurnCounter();

        for (int i = 0; i < pseudoCount; i++) {
            int m = pseudo[i];

            // Never allow “capturing” a king
            int capId = Move.capId(m);
            if (capId == Piece.WK.ordinal() + 1 || capId == Piece.BK.ordinal() + 1) continue;

            // Sanity: only accept side-to-move pieces
            Piece mover = Move.piece(m);
            if (mover == null || mover.isWhite() != whiteToMove) continue;

            board.makeMoveInternal(m);
            boolean kingSafe = whiteToMove ? !board.whiteInCheck : !board.blackInCheck;
            board.unmakeMoveInternal(m);

            if (kingSafe) {
                if (count < out.length) out[count++] = m;
                else return out.length;
            }
        }

        return count;
    }

    // ------------------------------------------------------------
    // Piece generators (append encoded int moves)
    // ------------------------------------------------------------

    private static int genKnights(Board board, int from, boolean isWhite, Masks masks, int[] out, int count) {
        long own   = isWhite ? board.getAllWhitePieces() : board.getAllBlackPieces();
        long enemy = isWhite ? board.getAllBlackPieces() : board.getAllWhitePieces();

        long targets = masks.getKnightMoves(from) & ~own;
        Piece mover  = isWhite ? Piece.WN : Piece.BN;

        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            targets &= targets - 1;

            Piece captured = (((enemy >>> to) & 1L) != 0) ? board.getPieceAtSquare(to) : null;

            if (count < out.length) {
                out[count++] = Move.pack(from, to, mover, captured, null, Move.FLAG_NORMAL);
            } else {
                return out.length;
            }
        }

        return count;
    }

    private static int genKings(Board board, int from, boolean isWhite, Masks masks, boolean includeCastling, int[] out, int count) {
        long own   = isWhite ? board.getAllWhitePieces() : board.getAllBlackPieces();
        long enemy = isWhite ? board.getAllBlackPieces() : board.getAllWhitePieces();

        long targets = masks.getKingMoves(from) & ~own;
        Piece mover  = isWhite ? Piece.WK : Piece.BK;

        if (includeCastling) {
            count = kingCastling(board, from, isWhite, out, count);
            if (count >= out.length) return out.length;
        }

        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            targets &= targets - 1;

            Piece captured = (((enemy >>> to) & 1L) != 0) ? board.getPieceAtSquare(to) : null;

            if (count < out.length) {
                out[count++] = Move.pack(from, to, mover, captured, null, Move.FLAG_NORMAL);
            } else {
                return out.length;
            }
        }

        return count;
    }

    private static int kingCastling(Board board, int curSquare, boolean isWhite, int[] out, int count) {
        long occupied   = board.getAllPieces();
        long attackMask = board.getAttackMask(!isWhite); // attacked by opponent

        if (isWhite) {
            if (board.whiteKingHasMoved || curSquare != 4) return count;

            // King-side (e1->g1)
            if (!board.whiteKingRookHasMoved && board.getPieceAtSquare(7) == Piece.WR) {
                boolean emptyF1 = (occupied & (1L << 5)) == 0;
                boolean emptyG1 = (occupied & (1L << 6)) == 0;

                boolean safeE1 = (attackMask & (1L << 4)) == 0;
                boolean safeF1 = (attackMask & (1L << 5)) == 0;
                boolean safeG1 = (attackMask & (1L << 6)) == 0;

                if (emptyF1 && emptyG1 && safeE1 && safeF1 && safeG1) {
                    if (count < out.length) out[count++] = Move.pack(4, 6, Piece.WK, null, null, Move.FLAG_CASTLE_KS);
                    else return out.length;
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
                    if (count < out.length) out[count++] = Move.pack(4, 2, Piece.WK, null, null, Move.FLAG_CASTLE_QS);
                    else return out.length;
                }
            }
        } else {
            if (board.blackKingHasMoved || curSquare != 60) return count;

            // King-side (e8->g8)
            if (!board.blackKingRookHasMoved && board.getPieceAtSquare(63) == Piece.BR) {
                boolean emptyF8 = (occupied & (1L << 61)) == 0;
                boolean emptyG8 = (occupied & (1L << 62)) == 0;

                boolean safeE8 = (attackMask & (1L << 60)) == 0;
                boolean safeF8 = (attackMask & (1L << 61)) == 0;
                boolean safeG8 = (attackMask & (1L << 62)) == 0;

                if (emptyF8 && emptyG8 && safeE8 && safeF8 && safeG8) {
                    if (count < out.length) out[count++] = Move.pack(60, 62, Piece.BK, null, null, Move.FLAG_CASTLE_KS);
                    else return out.length;
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
                    if (count < out.length) out[count++] = Move.pack(60, 58, Piece.BK, null, null, Move.FLAG_CASTLE_QS);
                    else return out.length;
                }
            }
        }

        return count;
    }

    private static int genPawns(Board board, int from, boolean isWhite, boolean pawnAttackMask, int[] out, int count) {
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
                    if (to >= 0 && to < 64) {
                        if (count < out.length) out[count++] = Move.pack(from, to, mover, null, null, Move.FLAG_NORMAL);
                        else return out.length;
                    }
                }
                if (file < 7) {
                    int to = from + 9;
                    if (to >= 0 && to < 64) {
                        if (count < out.length) out[count++] = Move.pack(from, to, mover, null, null, Move.FLAG_NORMAL);
                        else return out.length;
                    }
                }
            } else {
                if (file > 0) {
                    int to = from - 9;
                    if (to >= 0 && to < 64) {
                        if (count < out.length) out[count++] = Move.pack(from, to, mover, null, null, Move.FLAG_NORMAL);
                        else return out.length;
                    }
                }
                if (file < 7) {
                    int to = from - 7;
                    if (to >= 0 && to < 64) {
                        if (count < out.length) out[count++] = Move.pack(from, to, mover, null, null, Move.FLAG_NORMAL);
                        else return out.length;
                    }
                }
            }
            return count;
        }

        // -------------------------
        // NORMAL MOVE GEN
        // -------------------------

        // One step forward
        int fwd = from + 8 * dir;
        if (fwd >= 0 && fwd < 64 && ((occupied >>> fwd) & 1L) == 0) {
            if (promotionRank) {
                count = addPawnPromotions(from, fwd, mover, null, Move.FLAG_PAWN_PUSH, out, count);
                if (count >= out.length) return out.length;
            } else {
                if (count < out.length) out[count++] = Move.pack(from, fwd, mover, null, null, Move.FLAG_PAWN_PUSH);
                else return out.length;
            }

            // Two steps forward (only if one step was clear)
            boolean onStartRank = (isWhite && rank == 1) || (!isWhite && rank == 6);
            if (onStartRank) {
                int fwd2 = from + 16 * dir;
                if (fwd2 >= 0 && fwd2 < 64 && ((occupied >>> fwd2) & 1L) == 0) {
                    if (count < out.length) out[count++] = Move.pack(from, fwd2, mover, null, null, Move.FLAG_PAWN_PUSH);
                    else return out.length;
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
                    if (promotionRank) {
                        count = addPawnPromotions(from, to, mover, cap, Move.FLAG_NORMAL, out, count);
                        if (count >= out.length) return out.length;
                    } else {
                        if (count < out.length) out[count++] = Move.pack(from, to, mover, cap, null, Move.FLAG_NORMAL);
                        else return out.length;
                    }
                }
            }
            // NE: +9
            if (file < 7) {
                int to = from + 9;
                if (to >= 0 && to < 64 && ((enemyBB >>> to) & 1L) != 0) {
                    Piece cap = board.getPieceAtSquare(to);
                    if (promotionRank) {
                        count = addPawnPromotions(from, to, mover, cap, Move.FLAG_NORMAL, out, count);
                        if (count >= out.length) return out.length;
                    } else {
                        if (count < out.length) out[count++] = Move.pack(from, to, mover, cap, null, Move.FLAG_NORMAL);
                        else return out.length;
                    }
                }
            }
        } else {
            // SW: -9
            if (file > 0) {
                int to = from - 9;
                if (to >= 0 && to < 64 && ((enemyBB >>> to) & 1L) != 0) {
                    Piece cap = board.getPieceAtSquare(to);
                    if (promotionRank) {
                        count = addPawnPromotions(from, to, mover, cap, Move.FLAG_NORMAL, out, count);
                        if (count >= out.length) return out.length;
                    } else {
                        if (count < out.length) out[count++] = Move.pack(from, to, mover, cap, null, Move.FLAG_NORMAL);
                        else return out.length;
                    }
                }
            }
            // SE: -7
            if (file < 7) {
                int to = from - 7;
                if (to >= 0 && to < 64 && ((enemyBB >>> to) & 1L) != 0) {
                    Piece cap = board.getPieceAtSquare(to);
                    if (promotionRank) {
                        count = addPawnPromotions(from, to, mover, cap, Move.FLAG_NORMAL, out, count);
                        if (count >= out.length) return out.length;
                    } else {
                        if (count < out.length) out[count++] = Move.pack(from, to, mover, cap, null, Move.FLAG_NORMAL);
                        else return out.length;
                    }
                }
            }
        }

        // En passant
        int ep = board.getEnPassantSquare();
        if (ep != -1) {
            if (isWhite) {
                if (file > 0 && from + 7 == ep) {
                    if (count < out.length) out[count++] = Move.pack(from, ep, mover, null, null, Move.FLAG_EN_PASSANT);
                    else return out.length;
                }
                if (file < 7 && from + 9 == ep) {
                    if (count < out.length) out[count++] = Move.pack(from, ep, mover, null, null, Move.FLAG_EN_PASSANT);
                    else return out.length;
                }
            } else {
                if (file > 0 && from - 9 == ep) {
                    if (count < out.length) out[count++] = Move.pack(from, ep, mover, null, null, Move.FLAG_EN_PASSANT);
                    else return out.length;
                }
                if (file < 7 && from - 7 == ep) {
                    if (count < out.length) out[count++] = Move.pack(from, ep, mover, null, null, Move.FLAG_EN_PASSANT);
                    else return out.length;
                }
            }
        }

        return count;
    }

    private static int addPawnPromotions(int from, int to, Piece pawn, Piece capture, int flags, int[] out, int count) {
        Piece[] promos = pawn.isWhite()
                ? new Piece[]{Piece.WQ, Piece.WR, Piece.WB, Piece.WN}
                : new Piece[]{Piece.BQ, Piece.BR, Piece.BB, Piece.BN};

        for (Piece promo : promos) {
            if (count < out.length) {
                out[count++] = Move.pack(from, to, pawn, capture, promo, flags);
            } else {
                return out.length;
            }
        }
        return count;
    }

    private static int genBishops(Board board, int from, boolean isWhite, Masks masks, int[] out, int count) {
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

            if (count < out.length) {
                out[count++] = Move.pack(from, to, mover, captured, null, Move.FLAG_NORMAL);
            } else {
                return out.length;
            }
        }

        return count;
    }

    private static int genRooks(Board board, int from, boolean isWhite, Masks masks, int[] out, int count) {
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

            if (count < out.length) {
                out[count++] = Move.pack(from, to, mover, captured, null, Move.FLAG_NORMAL);
            } else {
                return out.length;
            }
        }

        return count;
    }

    private static int genQueens(Board board, int from, boolean isWhite, Masks masks, int[] out, int count) {
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

            if (count < out.length) {
                out[count++] = Move.pack(from, to, mover, captured, null, Move.FLAG_NORMAL);
            } else {
                return out.length;
            }
        }

        return count;
    }
}
