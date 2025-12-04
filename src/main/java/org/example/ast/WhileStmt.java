package org.example.ast;

public class WhileStmt implements Stmt {
    public final Expr condition;
    public final Stmt body;
    public final int line;
    public WhileStmt(Expr condition, Stmt body,int line) {
        this.condition = condition;
        this.body = body;
        this.line = line;
    }

    @Override
    public String toString() {
        return "WhileStmt{" +
                "condition=" + condition +
                ", body=" + body +
                '}';
    }
}