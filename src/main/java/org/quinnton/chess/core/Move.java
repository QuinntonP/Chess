package org.quinnton.chess.core;

public final class Move {

    /*
     FLAGS
     0 = movements that can be attacks
     1 = just movements like pawn pushes
     2 = castling queen side
     3 = castling king side
     4 = en-passant
     */

    public static final int FLAG_NORMAL      = 0; // normal move (quiet OR capture)
    public static final int FLAG_PAWN_PUSH   = 1; // pawn non-capture push
    public static final int FLAG_CASTLE_QS   = 2;
    public static final int FLAG_CASTLE_KS   = 3;
    public static final int FLAG_EN_PASSANT  = 4;


    // Bit layout:
    //  0.. 5  from (6)
    //  6..11  to (6)
    // 12..15  flags (4)
    // 16..19  pieceId (4)
    // 20..23  capId (4)
    // 24..27  promoId (4)

    private static final int FROM_SHIFT  = 0;
    private static final int TO_SHIFT    = 6;
    private static final int FLAGS_SHIFT = 12;
    private static final int PIECE_SHIFT = 16;
    private static final int CAP_SHIFT   = 20;
    private static final int PROMO_SHIFT = 24;

    private static final int SIX_BITS  = 0x3F;
    private static final int FOUR_BITS = 0x0F;

    private Move() {}

    // -------------------------------------------------------------------------
    // Packing
    // -------------------------------------------------------------------------
    public static int pack(int from, int to, Piece piece, Piece capture, Piece promo, int flags) {
        return ((from & SIX_BITS) << FROM_SHIFT)
                | ((to & SIX_BITS) << TO_SHIFT)
                | ((flags & FOUR_BITS) << FLAGS_SHIFT)
                | (((piece == null ? 0 : piece.id()) & FOUR_BITS) << PIECE_SHIFT)
                | (((capture == null ? 0 : capture.id()) & FOUR_BITS) << CAP_SHIFT)
                | (((promo == null ? 0 : promo.id()) & FOUR_BITS) << PROMO_SHIFT);
    }

    // -------------------------------------------------------------------------
    // Unpacking
    // -------------------------------------------------------------------------
    public static int from(int m)    { return (m >>> FROM_SHIFT)  & SIX_BITS; }
    public static int to(int m)      { return (m >>> TO_SHIFT)    & SIX_BITS; }
    public static int flags(int m)   { return (m >>> FLAGS_SHIFT) & FOUR_BITS; }

    public static int pieceId(int m) { return (m >>> PIECE_SHIFT) & FOUR_BITS; }
    public static int capId(int m)   { return (m >>> CAP_SHIFT)   & FOUR_BITS; }
    public static int promoId(int m) { return (m >>> PROMO_SHIFT) & FOUR_BITS; }

    public static Piece piece(int m)   { return Piece.fromId(pieceId(m)); }
    public static Piece capture(int m) { return Piece.fromId(capId(m)); }
    public static Piece promo(int m)   { return Piece.fromId(promoId(m)); }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    public static boolean isCapture(int m) {
        return capId(m) != 0 || flags(m) == FLAG_EN_PASSANT;
    }

    public static boolean isPromotion(int m) {
        return promoId(m) != 0;
    }

    public static boolean isCastle(int m) {
        int f = flags(m);
        return f == FLAG_CASTLE_QS || f == FLAG_CASTLE_KS;
    }

    public static boolean isEnPassant(int m) {
        return flags(m) == FLAG_EN_PASSANT;
    }

    // -------------------------------------------------------------------------
    // Debug / UCI
    // -------------------------------------------------------------------------
    public static String toUci(int m) {
        String s = sqToString(from(m)) + sqToString(to(m));
        if (isPromotion(m)) {
            s += Character.toLowerCase(promo(m).symbol);
        }
        return s;
    }

    private static String sqToString(int sq) {
        return "" + (char) ('a' + (sq & 7)) + (char) ('1' + (sq >>> 3));
    }
}
