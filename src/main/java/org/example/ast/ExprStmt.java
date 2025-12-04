package org.example.ast;


public class ExprStmt implements Stmt {
    public final Expr expr;
    public final int line;
    public ExprStmt(Expr expr,int line) { this.expr = expr;this.line = line; }

    @Override
    public String toString() {
        return "ExprStmt{" +
                "expr=" + expr +
                '}';
    }
}
