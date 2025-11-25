package org.quinnton.chess.core;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.Objects;
import java.util.Set;

public class BoardView {
    private final Board board;
    public final Canvas canvas;
    private final Color color1;
    private final Color color2;
    private final Color color3;
    private final int sqSize;
    private final GraphicsContext gc;
    private Set<Integer> highlights = Set.of();

    private long debugBitboard;

    public BoardView(Board board, int canvasSize, Color color1, Color color2, Color color3) {
        this.board = board;
        this.canvas = new Canvas(canvasSize, canvasSize);
        this.color1 = color1;
        this.color2 = color2;
        this.color3 = color3;
        this.sqSize = canvasSize / 8;
        this.gc = this.canvas.getGraphicsContext2D();
    }

    public void setHighlights(Set<Integer> squares) {
        this.highlights = (squares == null) ? Set.of() : squares;
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
        // Debug
        // drawBitboardOverlay();
    }

    private void drawSquare(int x, int y) {
        int square = (7 - y) * 8 + x;
        gc.setFill(((x + y) % 2 == 0) ? color1 : color2);
        gc.fillRect(x * sqSize, y * sqSize, sqSize, sqSize);

        if (highlights.contains(square)){
            gc.setFill(color3);
            gc.fillRect(x * sqSize, y * sqSize, sqSize, sqSize);
        }
    }

    private void drawPiece(int x, int y) {
        int square = (7 - y) * 8 + x;
        Piece piece = board.getPieceAtSquare(square);
        if (piece != null) {
            Image sprite = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/sprites/" + piece.name + ".png")
            ));
            gc.drawImage(sprite, x * sqSize, y * sqSize, sqSize, sqSize);
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


}
