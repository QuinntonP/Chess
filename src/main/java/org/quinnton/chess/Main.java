package org.quinnton.chess;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.quinnton.chess.bot.Bot;
import org.quinnton.chess.core.*;
import org.quinnton.chess.core.perft.Perft;
import org.quinnton.chess.core.perft.PerftPosition;
import org.quinnton.chess.core.perft.PerftRunner;

import java.util.List;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        Bot bot = new Bot(); // your search class
        Masks masks = new Masks();

        String fenString = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 ";

        // setup
        Board board = new Board(masks);
        board.loadFen(fenString);

        // =========================
        // Perft tests at startup
        // =========================


        PerftPosition startPos = new PerftPosition(
                "startpos",
                    fenString,
                new long[] {
                        1L,
                        20L,
                        400L,
                        8902L,
                        197281L,
                        4865609L
                }
        );

        System.out.println("Running test with FEN" + fenString);
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
        SelectionController controller = new SelectionController(board, view, bot);

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
