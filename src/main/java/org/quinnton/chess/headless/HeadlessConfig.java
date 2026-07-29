package org.quinnton.chess.headless;

import org.springframework.stereotype.Component;

@Component
public class HeadlessConfig {
    private int depth = 5;
    private long budgetMillis = 5000;
    private int maxPlies = HeadlessGameRunner.DEFAULT_MAX_PLIES;
    private boolean isWhite = true;
    private boolean logMoves = false;
    private String FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public void setDepth(int depth){
        this.depth = depth;
    }

    public int getDepth(){
        return this.depth;
    }

    public void setBudgetMillis(long budgetMillis){
        this.budgetMillis = budgetMillis;
    }

    public long getBudgetMillis(){
        return this.budgetMillis;
    }

    public void setMaxPlies(int maxPlies){
        this.maxPlies = maxPlies;
    }

    public int getMaxPlies(){
        return this.maxPlies;
    }

    public boolean getIsWhite(){
        return this.isWhite;
    }

    public void setIsWhite(boolean isWhite){
        this.isWhite = isWhite;
    }

    public boolean getLogMoves(){
        return this.logMoves;
    }

    public void setLogMoves(boolean logMoves){
        this.logMoves = logMoves;
    }

    public void setFEN(String FEN){
        this.FEN = FEN;
    }

    public String getFEN(){
        return FEN;
    }
}