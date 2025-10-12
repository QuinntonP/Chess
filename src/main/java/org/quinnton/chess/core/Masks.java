package org.quinnton.chess.core;

import java.util.ArrayList;
import java.util.HashMap;

public class Masks {
    public Masks(){
        onload();
    }

    // precomputed masks such as FILE_A, NOT_A_FILE, RANK_2, etc.
    static HashMap<Integer, Long> knightMoves = new HashMap<>();
    // --- File masks ---
    public static final long FILE_A = 0x0101010101010101L;
    public static final long FILE_B = 0x0202020202020202L;
    public static final long FILE_C = 0x0404040404040404L;
    public static final long FILE_D = 0x0808080808080808L;
    public static final long FILE_E = 0x1010101010101010L;
    public static final long FILE_F = 0x2020202020202020L;
    public static final long FILE_G = 0x4040404040404040L;
    public static final long FILE_H = 0x8080808080808080L;

    // --- Rank masks ---
    public static final long RANK_1 = 0x00000000000000FFL;
    public static final long RANK_2 = 0x000000000000FF00L;
    public static final long RANK_3 = 0x0000000000FF0000L;
    public static final long RANK_4 = 0x00000000FF000000L;
    public static final long RANK_5 = 0x000000FF00000000L;
    public static final long RANK_6 = 0x0000FF0000000000L;
    public static final long RANK_7 = 0x00FF000000000000L;
    public static final long RANK_8 = 0xFF00000000000000L;

    // --- Useful composite masks ---
    public static final long EDGES = FILE_A | FILE_H | RANK_1 | RANK_8;

    // Optional convenience masks
    public static final long NOT_A_FILE = 0xFEFEFEFEFEFEFEFEL;
    public static final long NOT_H_FILE = 0x7F7F7F7F7F7F7F7FL;

    // Pre generate tables
    private static final int[][] KNIGHT_DIRS = {
            { 2, 1}, { 1, 2}, {-1, 2}, {-2, 1},
            {-2,-1}, {-1,-2}, { 1,-2}, { 2,-1}
    };

    private static HashMap<Integer, Long> generateKnightMoves() {
        HashMap<Integer, Long> moves = new HashMap<Integer, Long>(64);

        for (int sq = 0; sq < 64; sq++) {
            int rank = sq / 8;
            int file = sq % 8;
            long mask = 0L;

            for (int[] d : KNIGHT_DIRS) {
                int r = rank + d[0];
                int f = file + d[1];
                if (r >= 0 && r < 8 && f >= 0 && f < 8) {
                    int targetSquare = r * 8 + f;
                    mask |= (1L << targetSquare);
                }
            }
            moves.put(sq, mask);
        }
        return moves;
    }



    public static void setKnightMoves(){
        knightMoves = generateKnightMoves();
    }

    public long getKnightMoves(int sq){
        return this.knightMoves.get(sq);
    }

    public static void onload(){
        generateKnightMoves();
        setKnightMoves();

    }
}