package org.quinnton.chess.core;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class BoardView {
    private final Board board;
    public final Canvas canvas;
    private final Color color1;
    private final Color color2;
    private final Color color3;
    private final Color checkColor;
    private final int sqSize;
    private final GraphicsContext gc;
    private Set<Integer> highlights = Set.of();
    private boolean drawPawnPromotion = false;
    private final Map<Piece, Image> spriteCache = new HashMap<>();



    private long debugBitboard;

    public BoardView(Board board, int canvasSize, Color color1, Color color2, Color color3, Color checkColor) {
        this.board = board;
        this.canvas = new Canvas(canvasSize, canvasSize);
        this.color1 = color1;
        this.color2 = color2;
        this.color3 = color3;
        this.checkColor = checkColor;
        this.sqSize = canvasSize / 8;
        this.gc = this.canvas.getGraphicsContext2D();
    }

    private Image spriteFor(Piece p) {


        return spriteCache.computeIfAbsent(p, piece ->
                new Image(Objects.requireNonNull(
                        getClass().getResourceAsStream("/sprites/" + piece.name + ".png")
                )));
    }


    public void setHighlights(Set<Integer> squares) {
        this.highlights = (squares == null) ? Set.of() : squares;
    }

    public void setDrawPawnPromotion(boolean show){
        this.drawPawnPromotion = show;
    }

    public void clearHighlights() {
        this.highlights = Set.of();
    }

    public int getSquareSize() {
        return this.sqSize;
    }

    public void draw() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                drawSquare(x, y);
                drawPiece(x, y);
            }
        }

        if (drawPawnPromotion){
            drawPawnPromotionWindow(board.getTurnCounter());
        }

        // Debug
//         drawBitboardOverlay();
    }

    private void drawSquare(int x, int y) {
        int square = (7 - y) * 8 + x;
        // draw board color
        gc.setFill(((x + y) % 2 == 0) ? color1 : color2);
        gc.fillRect(x * sqSize, y * sqSize, sqSize, sqSize);

        // draw checks
        if (board.whiteInCheck && board.whiteKingSquare == square){
            gc.setFill(checkColor);
            gc.fillRect(x * sqSize, y * sqSize, sqSize, sqSize);
        }

        if (board.blackInCheck && board.blackKingSquare == square){
            gc.setFill(checkColor);
            gc.fillRect(x * sqSize, y * sqSize, sqSize, sqSize);
        }

        // draw highlights
        if (highlights.contains(square)){
            gc.setFill(color3);
            gc.fillRect(x * sqSize, y * sqSize, sqSize, sqSize);
        }
    }

    private void drawPiece(int x, int y) {
        int square = (7 - y) * 8 + x;
        Piece piece = board.getPieceAtSquare(square);
        if (piece != null) {
            gc.drawImage(spriteFor(piece), x * sqSize, y * sqSize, sqSize, sqSize);
        }
    }


    public void setDebugBitboard(long bb){
        this.debugBitboard = bb;
    }


    private void drawBitboardOverlay() {
        long bb = debugBitboard;

        for (int sq = 0; sq < 64; sq++) {
            int rank = sq / 8;
            int file = sq % 8;

            boolean isSet = ((bb >>> sq) & 1L) != 0; // check bit at that square

            gc.setFill(isSet ? Color.DARKBLUE : Color.GRAY);
            gc.setFont(new Font("Arial", 24));
            gc.fillText(isSet ? "1" : "0", file * sqSize, (8 - rank) * sqSize);
        }
    }


    private void drawPawnPromotionWindow(boolean isWhite) {
        int xPos = sqSize * 3;
        int yPos = sqSize * 3;

        Piece[] pieces = isWhite
                ? new Piece[]{Piece.WQ, Piece.WR, Piece.WB, Piece.WN}
                : new Piece[]{Piece.BQ, Piece.BR, Piece.BB, Piece.BN};

        // background
        gc.setFill(Color.GOLD.deriveColor(1,1, 1, 0.8));
        gc.fillRect(xPos, yPos, sqSize * 2, sqSize * 2);

        // pieces
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {

                Piece p = pieces[x + y * 2];

                gc.drawImage(spriteFor(p), xPos + x * sqSize, yPos + y * sqSize, sqSize, sqSize);
            }
        }
    }

}
