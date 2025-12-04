package org.example.ast;

public class IfStmt implements Stmt {
    public final Expr condition;
    public final Stmt thenBranch;
    public final Stmt elseBranch;
    public final int line;// Может быть null

    public IfStmt(Expr condition, Stmt thenBranch, Stmt elseBranch,int line) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
        this.line = line;
    }

    @Override
    public String toString() {
        return "IfStmt{" +
                "condition=" + condition +
                ", thenBranch=" + thenBranch +
                ", elseBranch=" + elseBranch +
                '}';
    }
}