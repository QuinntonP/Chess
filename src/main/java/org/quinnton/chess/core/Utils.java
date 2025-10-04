package org.quinnton.chess.core;

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
}
