package org.quinnton.chess.core;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class BoardView {
    Board board;
    int canvasSize;
    public final Canvas canvas;
    Color color1;
    Color color2;
    int sqSize;
    GraphicsContext gc;

    public BoardView(Board board, int canvasSize, Color color1, Color color2){
        this.board = board;
        this.canvasSize = canvasSize;
        this.canvas = new Canvas(canvasSize, canvasSize);
        this.color1 = color1;
        this.color2 = color2;
        this.sqSize = canvasSize / 8;
        this.gc = this.canvas.getGraphicsContext2D();
    }

    public void draw(){
        for (int y = 0; y < 8; y++){
            for (int x = 0; x < 8; x++){
                drawSquares(x, y);
                drawPieces(x, y);
            }
        }

    }

    private void drawSquares(int x, int y){
        gc.setFill(((x + y) % 2 == 0) ? this.color1 : this.color2);

        int xPixel = x * this.sqSize;
        int yPixel = y * this.sqSize;

        gc.fillRect(xPixel, yPixel, sqSize, sqSize);
    }

    private void drawPieces(int x, int y){
        int square = (7 - y) * 8 + x;
        Piece piece = board.getPieceAtSquare(square);


        int xPixel = x * this.sqSize;
        int yPixel = y * this.sqSize;

        if (piece != null){
            String name = piece.name;
            Image sprite = new Image(getClass().getResourceAsStream("/sprites/" + name + ".png"));

            gc.drawImage(sprite, xPixel, yPixel, this.sqSize, this.sqSize);
        }
    }
}
