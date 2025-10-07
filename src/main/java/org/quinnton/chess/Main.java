package org.quinnton.chess;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.quinnton.chess.core.*;

import java.util.Set;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        Board board = new Board();
        board.loadFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR");

        int canvasSize = 800;
        BoardView view = new BoardView(board, canvasSize, Color.BEIGE, Color.TAN, Color.CORNFLOWERBLUE);
        Scene scene = new Scene(new Group(view.canvas), canvasSize, canvasSize);
        stage.setScene(scene);
        stage.setTitle("Chess");
        stage.show();

        final int[] selectedSq = { -1 };

        view.canvas.setOnMouseClicked(e -> {
            int sq = Utils.intFromCoordinates(e.getX(), e.getY(), view.getSquareSize());
            if (selectedSq[0] == -1) {
                Piece p = board.getPieceAtSquare(sq);
                if (p != null) {
                    selectedSq[0] = sq;
                    view.setHighlights(Set.of(selectedSq[0]));
                } else view.clearHighlights();
                return;
            }

            int first = selectedSq[0];
            Piece firstPiece = board.getPieceAtSquare(first);
            Piece secondPiece = board.getPieceAtSquare(sq);

            if (secondPiece != null && firstPiece != null && secondPiece.white == firstPiece.white) {
                selectedSq[0] = sq;
                view.setHighlights(Set.of(selectedSq[0]));
                return;
            }

            MoveList legalMoves = MoveGen.generate(first);
            selectedSq[0] = -1;
            view.clearHighlights();
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
