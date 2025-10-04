package org.quinnton.chess;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.quinnton.chess.core.Board;
import org.quinnton.chess.core.BoardView;
import org.quinnton.chess.core.Move;
import org.quinnton.chess.core.Piece;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        Board board = new Board();
        board.loadFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR");

        int canvasSize = 800;
        Color color1 = Color.BEIGE;
        Color color2 = Color.TAN;
        Color color3 = Color.CORNFLOWERBLUE;

        BoardView view = new BoardView(board, canvasSize, color1, color2, color3);

        Scene scene = new Scene(new Group(view.canvas), canvasSize, canvasSize);
        stage.setScene(scene);
        stage.setTitle("Chess");
        stage.show();

        // Draw once initially
        view.draw();

        // ===== Main Loop =====
        new AnimationTimer() {
            long last = 0;

            @Override
            public void handle(long now) {
                if (last == 0) {
                    last = now;
                    return;
                }

                double deltaTime = (now - last) / 1_000_000_000.0;
                last = now;

                // For now, we can just re-draw each frame
                view.draw();


                int click1 = view.getClick1Square();
                int click2 = view.getClick2Square();

                if (click1 != -1 && click2 != -1){
                    // move is ready to be made
                    // need to find out if it is a capture, promotion, not your own piece, even legal, etc.

                    view.resetClicks();
                }
            }
        }.start();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
