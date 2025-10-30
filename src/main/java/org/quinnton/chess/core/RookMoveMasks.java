package org.quinnton.chess.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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

            Long[] curBlockerMasks  = generateAllBlockerBitboards(mask);

            for(int i = 0; i < curBlockerMasks.length; i++){
                long legalMoves = getLegalMoves(sq, curBlockerMasks[i]);
                PositionBitboard positionBitboard = new PositionBitboard(sq, curBlockerMasks[i]);
                table.put(positionBitboard, legalMoves);
            }

        }


        return table;
    }

    public static Long[] generateAllBlockerBitboards(long mask) {
        List<Integer> moveSquareIndices = new ArrayList<>();

        for (int i = 0; i < 64; i++) {
            if (((mask >> i) & 1L) == 1L) {
                moveSquareIndices.add(i);
            }
        }

        int numPatterns = 1 << moveSquareIndices.size();
        Long[] blockerBitboards = new Long[numPatterns];
        Arrays.setAll(blockerBitboards, i -> 0L);

        for (int patternIndex = 0; patternIndex < numPatterns; patternIndex++) {
            for (int bitIndex = 0; bitIndex < moveSquareIndices.size(); bitIndex++) {
                int bit = (patternIndex >> bitIndex) & 1;
                if (bit == 1) {
                    blockerBitboards[patternIndex] |= (1L << moveSquareIndices.get(bitIndex));
                }
            }
        }
        return blockerBitboards;
    }

    private static long getLegalMoves(int startSquare, long bitboard) {
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


}
