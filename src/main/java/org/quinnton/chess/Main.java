package org.quinnton.chess;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.quinnton.chess.core.Board;
import org.quinnton.chess.core.BoardView;

public class Main extends Application {

    @Override
    public void start(Stage stage){
        Board board = new Board();
        board.loadFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR");

        Color color1 = Color.BEIGE;
        Color color2 = Color.TAN;

        BoardView view = new BoardView(board, 640, color1, color2);
        view.draw();  // <-- paint once

        Scene scene = new Scene(new Group(view.canvas), 640, 640); // <-- use the canvas
        stage.setScene(scene);
        stage.setTitle("Chess");
        stage.show();
    }

    public static void main(String[] args) {   // <-- proper main
        launch(args);
    }
}
