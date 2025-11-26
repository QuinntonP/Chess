package org.quinnton.chess.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.abs;

public class Utils {

    /**
     *
     * @param x coordinate X
     * @param y coordinate Y
     * @return Returns the square those coordinates fall on in bitboard notation (bottom left is 0 bottom right is 7)
     */
    public static int intFromCoordinates(double x, double y, int sqSize){

        double xSquare = Math.floor(x / sqSize);
        double ySquare = abs(Math.floor(y / sqSize) - 7);

        return Math.toIntExact(Math.round(xSquare + (ySquare * 8)));
    }

    /**
     *
     * @param bitboard bitboard
     * @return extracted squares in a list
     */
    public static ArrayList<Integer> extractSquares(long bitboard) {
        ArrayList<Integer> squares = new ArrayList<>(8);
        while (bitboard != 0) {
            int sq = Long.numberOfTrailingZeros(bitboard);
            squares.add(sq);
            bitboard &= bitboard - 1;
        }
        return squares;
    }

    /**
     *
     * @param mask a movement mask
     * @return all possible blocker bitboards for that mask
     */
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
}
