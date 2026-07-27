package org.quinnton.chess.headless;

import org.springframework.stereotype.Component;

@Component
public class HeadlessConfig {
    private int depth;
    private long budgetMillis;
    private int maxPlies;
    private boolean isWhite;
    private boolean logMoves;
    private String FEN;

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
        return getLogMoves();
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