package org.quinnton.chess;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.quinnton.chess.core.*;

public class Main extends Application {
    MoveList legalMoves;

    @Override
    public void start(Stage stage) {
        // setup
        Board board = new Board();
        board.loadFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR");

        Masks masks = new Masks();

        int canvasSize = 800;
        BoardView view = new BoardView(board, canvasSize, Color.BEIGE, Color.TAN, Color.CORNFLOWERBLUE);
        Scene scene = new Scene(new Group(view.canvas), canvasSize, canvasSize);
        stage.setScene(scene);
        stage.setTitle("Chess");
        stage.show();

        // clicking logic
        SelectionController controller = new SelectionController(board, view, masks);

        view.canvas.setOnMouseClicked(e -> {
            int sq = Utils.intFromCoordinates(e.getX(), e.getY(), view.getSquareSize());
            controller.onSquareClick(sq);
        });


        view.draw();

        new AnimationTimer() {
            long last = 0;
            @Override
            public void handle(long now) {
                if (last == 0) { last = now; return; }
                last = now;
                view.draw();
            }
        }.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
