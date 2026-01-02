package org.quinnton.chess.core;

public final class MagicTable {
    final long magic;     // the 64-bit magic number
    final int rbits;      // index width (popcount of blocker mask)
    final long mask;      // relevant blocker mask (inner rays)
    final long[] attacks; // size = 1 << rbits
    final int sizeMask;   // (1 << rbits) - 1

    MagicTable(long magic, int rbits, long mask, long[] attacks) {
        this.magic = magic;
        this.rbits = rbits;
        this.mask = mask;
        this.attacks = attacks;
        this.sizeMask = (1 << rbits) - 1;
    }
}

