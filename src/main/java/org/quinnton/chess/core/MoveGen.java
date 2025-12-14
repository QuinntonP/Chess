package org.quinnton.chess.core;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class MoveGen {

    public static MoveList generate(Board board, int curSquare, Piece firstPiece, Masks masks, boolean includeCastling, boolean pawnAttackMask){
        MoveList list;

        list = switch (firstPiece) {
            case WP, BP -> genPawns(board, curSquare, firstPiece.white, pawnAttackMask);
            case WN, BN -> genKnights(board, curSquare, firstPiece.white, masks);
            case WB, BB -> genBishops(board, curSquare, firstPiece.white, masks);
            case WR, BR -> genRooks(board, curSquare, firstPiece.white, masks);
            case WQ, BQ -> genQueens(board, curSquare, firstPiece.white, masks);
            case WK, BK -> genKings(board, curSquare, firstPiece.white, masks, includeCastling);
        };

        return  list;
    }

    private static MoveList genKnights(Board board, int curSquare, boolean isWhite, Masks masks) {
        MoveList list = new MoveList();
        long own = isWhite ? board.getAllWhitePieces() : board.getAllBlackPieces();
        long enemy = isWhite ? board.getAllBlackPieces() : board.getAllWhitePieces();

        long targets = masks.getKnightMoves(curSquare) & ~own; // can move to empty or enemy
        Piece mover = isWhite ? Piece.WN : Piece.BN;

        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            targets &= targets - 1;

            Piece captured = null;
            if (((enemy >>> to) & 1L) != 0) {
                // Only then pay the cost to resolve exact piece:
                captured = board.getPieceAtSquare(to);
            }
            list.add(new Move(curSquare, to, mover, captured, null, 0));
        }
        return list;
    }


    private static MoveList genKings(Board board, int curSquare, boolean isWhite, Masks masks, boolean includeCastling) {
        MoveList list = new MoveList();
        long own = isWhite ? board.getAllWhitePieces() : board.getAllBlackPieces();
        long enemy = isWhite ? board.getAllBlackPieces() : board.getAllWhitePieces();

        long targets = masks.getKingMoves(curSquare) & ~own;
        Piece mover = isWhite ? Piece.WK : Piece.BK;

        // castling
        if (includeCastling) {
            list = kingCastling(list, board, curSquare, isWhite);
        }

        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            targets &= targets - 1;

            Piece captured = ((enemy >>> to) & 1L) != 0 ? board.getPieceAtSquare(to) : null;
            list.add(new Move(curSquare, to, mover, captured, null, 0));
        }
        return list;
    }


    private static MoveList kingCastling(MoveList moveList, Board board, int curSquare, boolean isWhite) {
        long occupied   = board.getAllPieces();
        long attackMask = board.getAttackMask(!isWhite); // all squares attacked by the *opponent*

        if (isWhite) {
            // King must be on starting square and unmoved
            if (board.whiteKingHasMoved || curSquare != 4) {
                return moveList;
            }

            // ---------- White king-side castling (e1 -> g1) ----------
            if (!board.whiteKingRookHasMoved && board.getPieceAtSquare(7) == Piece.WR) {
                boolean emptyF1 = (occupied & (1L << 5)) == 0;
                boolean emptyG1 = (occupied & (1L << 6)) == 0;

                // e1, f1, g1 must NOT be attacked
                boolean safeE1 = (attackMask & (1L << 4)) == 0;
                boolean safeF1 = (attackMask & (1L << 5)) == 0;
                boolean safeG1 = (attackMask & (1L << 6)) == 0;

                if (emptyF1 && emptyG1 && safeE1 && safeF1 && safeG1) {
                    moveList.add(new Move(
                            4, 6,
                            Piece.WK,
                            null,
                            null,
                            3
                    ));
                }
            }

            // ---------- White queen-side castling (e1 -> c1) ----------
            if (!board.whiteQueenRookHasMoved && board.getPieceAtSquare(0) == Piece.WR) {
                boolean emptyB1 = (occupied & (1L << 1)) == 0;
                boolean emptyC1 = (occupied & (1L << 2)) == 0;
                boolean emptyD1 = (occupied & (1L << 3)) == 0;

                // e1, d1, c1 must NOT be attacked
                boolean safeE1 = (attackMask & (1L << 4)) == 0;
                boolean safeD1 = (attackMask & (1L << 3)) == 0;
                boolean safeC1 = (attackMask & (1L << 2)) == 0;

                if (emptyB1 && emptyC1 && emptyD1 && safeE1 && safeD1 && safeC1) {
                    moveList.add(new Move(
                            4, 2,
                            Piece.WK,
                            null,
                            null,
                            2
                    ));
                }
            }
        } else {
            // ---------- Black ----------
            if (board.blackKingHasMoved || curSquare != 60) {
                return moveList;
            }

            // ---------- Black king-side castling (e8 -> g8) ----------
            if (!board.blackKingRookHasMoved && board.getPieceAtSquare(63) == Piece.BR) {
                boolean emptyF8 = (occupied & (1L << 61)) == 0;
                boolean emptyG8 = (occupied & (1L << 62)) == 0;

                // e8, f8, g8 must NOT be attacked
                boolean safeE8 = (attackMask & (1L << 60)) == 0;
                boolean safeF8 = (attackMask & (1L << 61)) == 0;
                boolean safeG8 = (attackMask & (1L << 62)) == 0;

                if (emptyF8 && emptyG8 && safeE8 && safeF8 && safeG8) {
                    moveList.add(new Move(
                            60, 62,
                            Piece.BK,
                            null,
                            null,
                            3
                    ));
                }
            }

            // ---------- Black queen-side castling (e8 -> c8) ----------
            if (!board.blackQueenRookHasMoved && board.getPieceAtSquare(56) == Piece.BR) {
                boolean emptyB8 = (occupied & (1L << 57)) == 0;
                boolean emptyC8 = (occupied & (1L << 58)) == 0;
                boolean emptyD8 = (occupied & (1L << 59)) == 0;

                // e8, d8, c8 must NOT be attacked
                boolean safeE8 = (attackMask & (1L << 60)) == 0;
                boolean safeD8 = (attackMask & (1L << 59)) == 0;
                boolean safeC8 = (attackMask & (1L << 58)) == 0;

                if (emptyB8 && emptyC8 && emptyD8 && safeE8 && safeD8 && safeC8) {
                    moveList.add(new Move(
                            60, 58,
                            Piece.BK,
                            null,
                            null,
                            2
                    ));
                }
            }
        }

        return moveList;
    }




    private static MoveList genPawns(Board board, int curSquare, boolean isWhite, boolean pawnAttackMask) {
        MoveList list = new MoveList();

        int rank = curSquare / 8;
        int file = curSquare % 8;

        boolean promotion = (isWhite && rank == 6) || (!isWhite && rank == 1);

        long occupied = board.getAllPieces();
        long enemy    = isWhite ? board.getAllBlackPieces() : board.getAllWhitePieces();
        Piece mover   = isWhite ? Piece.WP : Piece.BP;
        int dir       = isWhite ? 1 : -1;

        // ---------- One step forward ----------
        int fwd = curSquare + 8 * dir;
        if (fwd >= 0 && fwd < 64 && ((occupied >>> fwd) & 1L) == 0) {

            // check for promotion
            if (promotion){
                list = addPawnPromotion(list, curSquare, fwd, mover, null, 1);
            }
            else{
                list.add(new Move(curSquare, fwd, mover, null, null, 1));
            }

            // ---------- Two steps forward (only from starting rank) ----------
            boolean onStartRank = (isWhite && rank == 1) || (!isWhite && rank == 6);
            if (onStartRank) {
                int fwd2 = curSquare + 16 * dir;
                if (fwd2 >= 0 && fwd2 < 64 && ((occupied >>> fwd2) & 1L) == 0) {
                    list.add(new Move(curSquare, fwd2, mover, null, null, 1));
                }
            }
        }

        // ---------- Captures ----------
        long enemyBB = enemy;if (isWhite) {
            // White captures: NW (+7) and NE (+9)
            if (file > 0) {
                int left = curSquare + 7; // file-1, rank+1
                if (left >= 0 && left < 64 && ((enemyBB >>> left) & 1L) != 0 || pawnAttackMask) {
                    Piece captured = board.getPieceAtSquare(left);

                    // check promotion
                    if (promotion){
                        list = addPawnPromotion(list, curSquare, left, mover, captured, 0);
                    }
                    else{
                        list.add(new Move(curSquare, left, mover, captured, null, 0));
                    }
                }
            }    if (file < 7) {
                int right = curSquare + 9; // file+1, rank+1
                if (right >= 0 && right < 64 && ((enemyBB >>> right) & 1L) != 0 || pawnAttackMask) {
                    Piece captured = board.getPieceAtSquare(right);

                    // check promotion
                    if (promotion){
                        list = addPawnPromotion(list, curSquare, right, mover, captured, 0);
                    }
                    else{
                        list.add(new Move(curSquare, right, mover, captured, null, 0));
                    }

                }
            }
        } else {
            // Black captures: SW (-9) and SE (-7)
            if (file > 0) {
                int left = curSquare - 9; // file-1, rank-1
                if (left >= 0 && left < 64 && ((enemyBB >>> left) & 1L) != 0 || pawnAttackMask) {
                    Piece captured = board.getPieceAtSquare(left);

                    // check promotion
                    if (promotion){
                        list = addPawnPromotion(list, curSquare, left, mover, captured, 0);
                    }
                    else{
                        list.add(new Move(curSquare, left, mover, captured, null, 0));
                    }

                }
            }    if (file < 7) {
                int right = curSquare - 7; // file+1, rank-1
                if (right >= 0 && right < 64 && ((enemyBB >>> right) & 1L) != 0 || pawnAttackMask) {
                    Piece captured = board.getPieceAtSquare(right);

                    // check promotion
                    if (promotion){
                        list = addPawnPromotion(list, curSquare, right, mover, captured, 0);
                    }
                    else{
                        list.add(new Move(curSquare, right, mover, captured, null, 0));
                    }

                }
            }
        }

        // ---------- En passant ----------
        int ep = board.getEnPassantSquare();
        if (ep == -1) {
            // no en passant possible
            return list;
        }

        // int rank = curSquare / 8; // optional if you want an extra rank guard

        if (isWhite) {
            // left white capture: +7 (file-1, rank+1)
            if (file > 0 && curSquare + 7 == ep) {
                list.add(new Move(curSquare, ep, mover, null, null, 5)); // flag 5 = EP
            }

            // right white capture: +9 (file+1, rank+1)
            if (file < 7 && curSquare + 9 == ep) {
                list.add(new Move(curSquare, ep, mover, null, null, 5));
            }
        } else {
            // left black capture: -9 (file-1, rank-1)
            if (file > 0 && curSquare - 9 == ep) {
                list.add(new Move(curSquare, ep, mover, null, null, 5));
            }

            // right black capture: -7 (file+1, rank-1)
            if (file < 7 && curSquare - 7 == ep) {
                list.add(new Move(curSquare, ep, mover, null, null, 5));
            }
        }


        return list;
    }


    private static MoveList addPawnPromotion(MoveList moveList, int from, int to, Piece piece, Piece capture, int flags){
        Piece[] pieces = piece.isWhite()
                ? new Piece[]{Piece.WQ, Piece.WR, Piece.WB, Piece.WN}
                : new Piece[]{Piece.BQ, Piece.BR, Piece.BB, Piece.BN};

        for (Piece promoPiece : pieces){
            Move promoMove = new Move(from, to, piece, capture, promoPiece, flags);
            moveList.add(promoMove);
        }

        return moveList;
    }



    private static MoveList genBishops(Board board, int curSquare, boolean isWhite, Masks masks){
        MoveList list = new MoveList();

        long own   = isWhite ? board.getAllWhitePieces() : board.getAllBlackPieces();
        long enemy = isWhite ? board.getAllBlackPieces() : board.getAllWhitePieces();

        long occupied = board.getAllPieces();

        long mask   = BishopMoveMasks.bishopBlockerMask(curSquare);
        long subset = occupied & mask;  // relevant blockers only

        long targets = masks.getBishopMoves(curSquare, subset);

        // Remove own pieces
        targets &= ~own;

        Piece mover = isWhite ? Piece.WB : Piece.BB;
        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            targets &= targets - 1;

            Piece captured = ((enemy >>> to) & 1L) != 0 ? board.getPieceAtSquare(to) : null;
            list.add(new Move(curSquare, to, mover, captured, null, 0));
        }

        return list;
    }

    private static MoveList genQueens(Board board, int curSquare, boolean isWhite, Masks masks){
        MoveList list = new MoveList();

        long own   = isWhite ? board.getAllWhitePieces() : board.getAllBlackPieces();
        long enemy = isWhite ? board.getAllBlackPieces() : board.getAllWhitePieces();

        long occupied = board.getAllPieces();

        long bishopMask   = BishopMoveMasks.bishopBlockerMask(curSquare);
        long rookMask     = RookMoveMasks.rookBlockerMask(curSquare);

        long bishopSubset = occupied & bishopMask;  // relevant blockers only
        long rookSusbet = occupied & rookMask;

        long bishopTargets = masks.getBishopMoves(curSquare, bishopSubset);
        long rookTargets = masks.getRookMoves(curSquare, rookSusbet);

        long targets = bishopTargets | rookTargets;

        // Remove own pieces
        targets &= ~own;

        Piece mover = isWhite ? Piece.WQ : Piece.BQ;
        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            targets &= targets - 1;

            Piece captured = ((enemy >>> to) & 1L) != 0 ? board.getPieceAtSquare(to) : null;
            list.add(new Move(curSquare, to, mover, captured, null, 0));
        }

        return list;
    }

    private static MoveList genRooks(Board board, int curSquare, boolean isWhite, Masks masks) {
        MoveList list = new MoveList();

        long own   = isWhite ? board.getAllWhitePieces() : board.getAllBlackPieces();
        long enemy = isWhite ? board.getAllBlackPieces() : board.getAllWhitePieces();

        long occupied = board.getAllPieces();

        long mask   = RookMoveMasks.rookBlockerMask(curSquare);
        long subset = occupied & mask;  // relevant blockers only

        long targets = masks.getRookMoves(curSquare, subset);

        // Remove own pieces
        targets &= ~own;

        Piece mover = isWhite ? Piece.WR : Piece.BR;
        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            targets &= targets - 1;

            Piece captured = ((enemy >>> to) & 1L) != 0 ? board.getPieceAtSquare(to) : null;
            list.add(new Move(curSquare, to, mover, captured, null, 0));
        }

        return list;
    }

    public static HashMap<Integer, MoveList> generatePseudoLegalMoves(Board board, Masks masks, boolean includeCastling) {
        HashMap<Integer, MoveList> allMoves = new HashMap<>();

        boolean whiteToMove = board.getTurnCounter();
        long bb = whiteToMove ? board.getAllWhitePieces() : board.getAllBlackPieces();

        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            bb &= bb - 1;

            Piece curPiece = board.getPieceAtSquare(sq);
            if (curPiece == null) continue;

            // Extra safety: make sure this piece matches side-to-move
            if (curPiece.isWhite() != whiteToMove) continue;

            MoveList curMoves = generate(board, sq, curPiece, masks, includeCastling, false);
            if (!curMoves.isEmpty()) {
                allMoves.put(sq, curMoves);
            }
        }

        return allMoves;
    }


    public static HashMap<Integer, MoveList> generatePseudoLegalMoves(Board board, Masks masks, Long bitboard, boolean includeCastling, boolean pawnAttackMask){
        HashMap<Integer, MoveList> allMoves = new HashMap<>();

        long bb = bitboard;

        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);  // gets index of lowest 1-bit

            Piece curPiece = board.getPieceAtSquare(sq);

            if (curPiece == null) {continue;}

            MoveList curMoves = generate(board, sq, curPiece, masks, includeCastling, pawnAttackMask);

            allMoves.put(sq, curMoves);

            bb &= bb - 1;  // removes the lowest 1-bit
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
                // Never allow “capturing” a king
                if (move.capture == Piece.WK || move.capture == Piece.BK) {
                    continue;
                }

                // Sanity: should already be true, but keep it
                if (move.piece.isWhite() != whiteToMove) {
                    continue;
                }

                board.makeMoveInternal(move);

                boolean kingSafe = whiteToMove ? !board.whiteInCheck : !board.blackInCheck;
                if (kingSafe) {
                    dstList.add(move);
                }

                board.unmakeMoveInternal(move);
            }

            if (!dstList.isEmpty()) {
                legal.put(fromSq, dstList);
            }
        }

        return legal;
    }

}
