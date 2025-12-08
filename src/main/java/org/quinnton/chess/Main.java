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
        board.loadFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR");

        // =========================
        // Perft tests at startup
        // =========================

        PerftPosition startPos = new PerftPosition(
                "startpos",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                new long[] {
                        1L,      // depth 0
                        20L,     // depth 1
                        400L,    // depth 2
                        8902L,   // depth 3
                        197281  // depth 5
                }
        );

        System.out.println("=== Running perft suite ===");
        PerftRunner.runPerftSuite(List.of(startPos), masks);

        // If you want a simple root breakdown for visual debugging:
        System.out.println("=== Perft root breakdown (depth 2 from current board) ===");
        Perft.perftRoot(board, masks, 4);

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
