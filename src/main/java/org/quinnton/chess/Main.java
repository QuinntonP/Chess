package org.quinnton.chess;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.quinnton.chess.core.*;
import org.quinnton.chess.core.perft.Perft;
import org.quinnton.chess.core.perft.PerftPosition;
import org.quinnton.chess.core.perft.PerftRunner;

import java.util.List;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        Masks masks = new Masks();

        // setup
        Board board = new Board(masks);
        board.loadFen("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1 ");

        // =========================
        // Perft tests at startup
        // =========================

        PerftPosition startPos = new PerftPosition(
                "startpos",
                "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1 ",
                new long[] {
                        1L,        // depth 0
                        14L,       // depth 1
                        191L,
                        2812L,
                        43238L,
                        674624L
                }
        );

        System.out.println("=== Running perft suite ===");
        PerftRunner.runPerftSuite(List.of(startPos), masks);

        Perft.perftRoot(board, masks, 3);

        int canvasSize = 800;
        BoardView view = new BoardView(board, canvasSize, Color.BEIGE, Color.TAN, Color.CORNFLOWERBLUE.deriveColor(0, 1, 1, 0.6), Color.RED.deriveColor(0, 1, 1, 0.6));
        Scene scene = new Scene(new Group(view.canvas), canvasSize, canvasSize);
        stage.setScene(scene);
        stage.setTitle("Chess");
        stage.show();


        // clicking logic
        SelectionController controller = new SelectionController(board, view);

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
                view.setDebugBitboard(Masks.ROOK_MOVE_MASK);
                view.draw();
            }
        }.start();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
