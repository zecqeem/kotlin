package org.example.ast;


public class ExprStmt implements Stmt {
    public final Expr expr;
    public ExprStmt(Expr expr) { this.expr = expr; }

    @Override
    public String toString() {
        return "ExprStmt{" +
                "expr=" + expr +
                '}';
    }
}
