package org.quinnton.chess.core;

import java.util.HashMap;

public class RookMoveMasks {

    // Build lookup table
    public static HashMap<PositionBitboard, Long> generateRookLookupTable() {
        HashMap<PositionBitboard, Long> table = new HashMap<>();

        // create indices
        for (int sq = 0; sq < 64; sq++) {
            int rank = sq / 8;
            int file = sq % 8;

            long curSquareMask = 1L << sq;  // correct per A1=LSB layout
            long mask = (Masks.RANKS[rank] | Masks.FILES[file]) & ~curSquareMask;
            mask &= rookBlockerMask(sq);

            Long[] curBlockerMasks  = Utils.generateAllBlockerBitboards(mask);

            for(int i = 0; i < curBlockerMasks.length; i++){
                long legalMoves = getLegalMoves(sq, curBlockerMasks[i]);
                PositionBitboard positionBitboard = new PositionBitboard(sq, curBlockerMasks[i]);
                table.put(positionBitboard, legalMoves);
            }

        }

        System.out.println("Rook table size: " + table.size());
        return table;
    }


    public static long getLegalMoves(int startSquare, long bitboard) {
        long moves = 0L;
        int rank = startSquare / 8;
        int file = startSquare % 8;

        // North (up)
        for (int r = rank + 1; r < 8; r++) {
            int sq = r * 8 + file;
            moves |= (1L << sq);
            if (((bitboard >>> sq) & 1L) != 0) break; // stop if blocked
        }

        // South (down)
        for (int r = rank - 1; r >= 0; r--) {
            int sq = r * 8 + file;
            moves |= (1L << sq);
            if (((bitboard >>> sq) & 1L) != 0) break;
        }

        // East (right)
        for (int f = file + 1; f < 8; f++) {
            int sq = rank * 8 + f;
            moves |= (1L << sq);
            if (((bitboard >>> sq) & 1L) != 0) break;
        }

        // West (left)
        for (int f = file - 1; f >= 0; f--) {
            int sq = rank * 8 + f;
            moves |= (1L << sq);
            if (((bitboard >>> sq) & 1L) != 0) break;
        }

        return moves;
    }

    public static long rookBlockerMask(int sq) {
        int r = sq / 8, f = sq % 8;
        long m = 0L;

        // north (stop before rank 7)
        for (int rr = r + 1; rr <= 6; rr++) m |= 1L << (rr * 8 + f);
        // south (stop after rank 0)
        for (int rr = r - 1; rr >= 1; rr--) m |= 1L << (rr * 8 + f);
        // east (stop before file 7)
        for (int ff = f + 1; ff <= 6; ff++) m |= 1L << (r * 8 + ff);
        // west (stop after file 0)
        for (int ff = f - 1; ff >= 1; ff--) m |= 1L << (r * 8 + ff);

        return m;
    }




}
