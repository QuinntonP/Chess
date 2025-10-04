package org.quinnton.chess.core;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import jdk.jshell.execution.Util;

import java.util.Objects;

public class BoardView {
    Board board;
    int canvasSize;
    public final Canvas canvas;
    Color color1;
    Color color2;
    Color color3;
    int sqSize;
    GraphicsContext gc;

    int click1Sq = -1;
    int click2Sq = -1;

    public BoardView(Board board, int canvasSize, Color color1, Color color2, Color color3){
        this.board = board;
        this.canvasSize = canvasSize;
        this.canvas = new Canvas(canvasSize, canvasSize);
        this.color1 = color1;
        this.color2 = color2;
        this.color3 = color3;
        this.sqSize = canvasSize / 8;
        this.gc = this.canvas.getGraphicsContext2D();

        this.canvas.setOnMouseClicked(e -> onMouseClick(e.getX(), e.getY()));
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
        int square = (7 - y) * 8 + x;
        gc.setFill(((x + y) % 2 == 0) ? this.color1 : this.color2);

        // Highlighted squares
        if (square == click1Sq || square == click2Sq) gc.setFill(this.color3);

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
            Image sprite = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/sprites/" + name + ".png")));

            gc.drawImage(sprite, xPixel, yPixel, this.sqSize, this.sqSize);
        }
    }

    private void onMouseClick(double x, double y){
        int square = Utils.intFromCoordinates(x, y, this.sqSize);

        // resets clicks
        if (click2Sq != -1){
            click1Sq = -1;
            click2Sq = -1;
            return;
        }

        // first click
        if (click1Sq == -1){
            click1Sq = square;
            return;
        }

        // second click
        click2Sq = square;

        // if first click is an empty square ignore
        if (board.getPieceAtSquare(click1Sq) == null){
            resetClicks();
        }

        // if second click is the same color then make click 1 click 2
        if(board.getPieceAtSquare(click2Sq) != null){
            if (board.getPieceAtSquare(click2Sq).white == board.getPieceAtSquare(click1Sq).white){
                click1Sq = click2Sq;
                click2Sq = -1;
            }
        }
    }

    public void resetClicks(){
        this.click1Sq = -1;
        this.click2Sq = -1;
    }

    public int getClick1Square(){
        return this.click1Sq;
    }

    public int getClick2Square(){
        return this.click2Sq;
    }
}
