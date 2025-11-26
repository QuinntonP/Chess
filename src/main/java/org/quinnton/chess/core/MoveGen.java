package org.quinnton.chess.core;


public final class MoveGen {

    public static MoveList generate(Board board, int curSquare, Piece firstPiece, Masks masks){
        MoveList list = new MoveList();

        // test
        Move testMove = new Move(curSquare, 30, firstPiece, Piece.BP, null, 0);
        list.add(testMove);

        list = switch (firstPiece) {
            case WP, BP -> genPawns(board, curSquare, firstPiece.white);
            case WN, BN -> genKnights(board, curSquare, firstPiece.white, masks);
            case WB, BB -> genBishops(board, curSquare, firstPiece.white, masks);
            case WR, BR -> genRooks(board, curSquare, firstPiece.white, masks);
            case WQ, BQ -> genQueens(board, curSquare, firstPiece.white, masks);
            case WK, BK -> genKings(board, curSquare, firstPiece.white, masks);
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


    private static MoveList genKings(Board board, int curSquare, boolean isWhite, Masks masks) {
        MoveList list = new MoveList();
        long own = isWhite ? board.getAllWhitePieces() : board.getAllBlackPieces();
        long enemy = isWhite ? board.getAllBlackPieces() : board.getAllWhitePieces();

        long targets = masks.getKingMoves(curSquare) & ~own;
        Piece mover = isWhite ? Piece.WK : Piece.BK;

        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            targets &= targets - 1;

            Piece captured = ((enemy >>> to) & 1L) != 0 ? board.getPieceAtSquare(to) : null;
            list.add(new Move(curSquare, to, mover, captured, null, 0));
        }
        return list;
    }


    private static MoveList genPawns(Board board, int curSquare, boolean isWhite){
        int rank = curSquare / 8;

        MoveList list = new MoveList();

        long mask;

        long own   = isWhite ? board.getAllWhitePieces() : board.getAllBlackPieces();
        long enemy = isWhite ? board.getAllBlackPieces() : board.getAllWhitePieces();

        long occupied = board.getAllPieces();
        long targets = 0L;

        Piece mover = isWhite ? Piece.WP : Piece.BP;

        int direction = 1;

        if(!isWhite){
            direction = -1;
        }

        // move 1 forward
        mask = 1L << (8 * direction + curSquare);
        if ((mask & occupied) == 0){
            targets |= mask;

            //  move forward 2
            mask = 1L << (16 * direction + curSquare);
            if ((mask & occupied) == 0 && (rank == 1 || rank == 6)){
                targets |= mask;
            }
        }

        // left capture
        mask = 1L << (7 * direction + curSquare);
        if ((mask & enemy) != 0){
            targets |= mask;
        }
        // right capture
        mask = 1L << (9 * direction + curSquare);
        if ((mask & enemy) != 0){
            targets |= mask;
        }

        // dang enpassant
        long enemyPawns = enemy & board.getAllPawns();

        Move myPreviousMove = isWhite ? board.getLastWhiteMove() : board.getLastBlackMove();

        if (((isWhite && rank == 4) || (!isWhite && rank == 3)) && myPreviousMove.to == curSquare){
            // right
            mask = 1L << curSquare + 1;
            if ((enemyPawns & mask) != 0){
                targets |= mask;
            }

            // left
            mask = 1L << curSquare -1;
            if ((enemyPawns & mask) != 0){
                targets |= mask;
            }

        }

        while (targets != 0) {
            int to = Long.numberOfTrailingZeros(targets);
            targets &= targets - 1;

            Piece captured = ((enemy >>> to) & 1L) != 0 ? board.getPieceAtSquare(to) : null;
            list.add(new Move(curSquare, to, mover, captured, null, 0));
        }

        return list;
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

        Piece mover = isWhite ? Piece.WR : Piece.BR;
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

        Piece mover = isWhite ? Piece.WR : Piece.BR;
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




}
