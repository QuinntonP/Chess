package org.quinnton.chess.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class BishopMoveMasks {

    // Build lookup table
    public static HashMap<PositionBitboard, Long> generateBishopLookupTable() {
        HashMap<PositionBitboard, Long> table = new HashMap<>();

        // create indices
        for (int sq = 0; sq < 64; sq++){
            long curSquareMask = 1L << sq;  // correct per A1=LSB layout
            long mask = Masks.getDiagonalMasks(sq) &~ curSquareMask;
            mask &= bishopBlockerMask(sq);

            Long[] curBlockerMasks  = Utils.generateAllBlockerBitboards(mask);

            for(int i = 0; i < curBlockerMasks.length; i++){
                long legalMoves = getBishopMoves(sq, curBlockerMasks[i]);
                PositionBitboard positionBitboard = new PositionBitboard(sq, curBlockerMasks[i]);
                table.put(positionBitboard, legalMoves);
            }
        }

        System.out.println("Bishop table size: " + table.size());
        return table;
    }



    public static long getBishopMoves(int startSquare, long bitboard) {
        long moves = 0L;
        int rank = startSquare / 8;
        int file = startSquare % 8;

        // North-East (up-right)
        for (int r = rank + 1, f = file + 1; r < 8 && f < 8; r++, f++) {
            int sq = r * 8 + f;
            moves |= (1L << sq);
            if (((bitboard >>> sq) & 1L) != 0) break; // stop if blocked
        }

        // North-West (up-left)
        for (int r = rank + 1, f = file - 1; r < 8 && f >= 0; r++, f--) {
            int sq = r * 8 + f;
            moves |= (1L << sq);
            if (((bitboard >>> sq) & 1L) != 0) break;
        }

        // South-East (down-right)
        for (int r = rank - 1, f = file + 1; r >= 0 && f < 8; r--, f++) {
            int sq = r * 8 + f;
            moves |= (1L << sq);
            if (((bitboard >>> sq) & 1L) != 0) break;
        }

        // South-West (down-left)
        for (int r = rank - 1, f = file - 1; r >= 0 && f >= 0; r--, f--) {
            int sq = r * 8 + f;
            moves |= (1L << sq);
            if (((bitboard >>> sq) & 1L) != 0) break;
        }

        return moves;
    }

    public static long bishopBlockerMask(int sq) {
        int r = sq / 8, f = sq % 8;
        long m = 0L;

        // NE (up-right) — stop before hitting rank 7 or file 7
        for (int rr = r + 1, ff = f + 1; rr <= 6 && ff <= 6; rr++, ff++) {
            m |= 1L << (rr * 8 + ff);
        }

        // NW (up-left) — stop before hitting rank 7 or file 0
        for (int rr = r + 1, ff = f - 1; rr <= 6 && ff >= 1; rr++, ff--) {
            m |= 1L << (rr * 8 + ff);
        }

        // SE (down-right) — stop before hitting rank 0 or file 7
        for (int rr = r - 1, ff = f + 1; rr >= 1 && ff <= 6; rr--, ff++) {
            m |= 1L << (rr * 8 + ff);
        }

        // SW (down-left) — stop before hitting rank 0 or file 0
        for (int rr = r - 1, ff = f - 1; rr >= 1 && ff >= 1; rr--, ff--) {
            m |= 1L << (rr * 8 + ff);
        }

        return m;
    }


}
