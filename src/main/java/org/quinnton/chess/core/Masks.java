package org.quinnton.chess.core;

import java.util.HashMap;

public class Masks {
    public Masks(){
        onload();
    }

    // precomputed masks such as FILE_A, NOT_A_FILE, RANK_2, etc.
    static HashMap<Integer, Long> knightMoves = new HashMap<>();
    static HashMap<Integer, Long> kingMoves = new HashMap<>();
    static HashMap<PositionBitboard, Long> rookMoves = new HashMap<>();

    // Move Masks
    public static long QUEEN_MOVE_MASK = 0;
    public static long KING_MOVE_MASK = 0;
    public static long BISHOP_MOVE_MASK = 0;
    public static long KNIGHT_MOVE_MASK = 0;
    public static long ROOK_MOVE_MASK = 0;
    public static long PAWN_MOVE_MASK = 0;


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

    public static final long NOT_FILE_A  = ~FILE_A;
    public static final long NOT_FILE_H  = ~FILE_H;
    public static final long NOT_FILE_AB = ~(FILE_A | FILE_B);
    public static final long NOT_FILE_GH = ~(FILE_G | FILE_H);


    public static final long[] RANKS = {RANK_1, RANK_2, RANK_3, RANK_4, RANK_5, RANK_6, RANK_7, RANK_8};
    public static final long[] FILES = {FILE_A, FILE_B, FILE_C, FILE_D, FILE_E, FILE_F, FILE_G, FILE_H};

    // --- Useful composite masks ---
    public static final long EDGES = FILE_A | FILE_H | RANK_1 | RANK_8;



    // Pre generate tables
    private static final int[][] KNIGHT_DIRS = {
            { 2, 1}, { 1, 2}, {-1, 2}, {-2, 1},
            {-2,-1}, {-1,-2}, { 1,-2}, { 2,-1}
    };

    private static final int[][] KING_DIRS = {
            {-1, 0}, {-1, 1}, {0, 1}, {1, 1},
            {1, 0}, {1, -1}, {0, -1}, {-1, -1}
    };


    public long getRookMoveMask(long blockerMask){

        return 1L;
    }


    private void generateRookMoves(){

    }


    private static void generateKingMoves() {
        HashMap<Integer, Long> moves = new HashMap<Integer, Long>(64);

        for (int sq = 0; sq < 64; sq++) {
            int rank = sq / 8;
            int file = sq % 8;
            long mask = 0L;

            for (int[] d : KING_DIRS) {
                int r = rank + d[0];
                int f = file + d[1];
                if (r >= 0 && r < 8 && f >= 0 && f < 8) {
                    int targetSquare = r * 8 + f;
                    mask |= (1L << targetSquare);
                }
            }
            moves.put(sq, mask);
        }
        kingMoves = moves;
    }




    private static void generateKnightMoves() {
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
        knightMoves = moves;
    }


    public long getKnightMoves(int sq){
        return this.knightMoves.get(sq);
    }

    public static void onload(){
        generateKnightMoves();
        generateKingMoves();
        rookMoves = RookMoveMasks.generateRookLookupTable();
    }


    public long getKingMoves(int sq) {
        return this.kingMoves.get(sq);
    }


    public long getRookMoves(int sq, long blockerBitboard) {
        return this.rookMoves.get(new PositionBitboard(sq, blockerBitboard));
    }


    public void setMoveMask(Piece piece, long mask){
        switch (piece){
            case WP, BP -> PAWN_MOVE_MASK = mask;
            case WN, BN -> KNIGHT_MOVE_MASK = mask;
            case WB, BB -> BISHOP_MOVE_MASK = mask;
            case WR, BR -> ROOK_MOVE_MASK =  mask;
            case WQ, BQ -> QUEEN_MOVE_MASK = mask;
            case WK, BK -> KING_MOVE_MASK = mask;
        }
    }


    public static long getFile(int i){
        return FILES[i];
    }

    public static long getRanks(int i){
        return RANKS[i];
    }
}