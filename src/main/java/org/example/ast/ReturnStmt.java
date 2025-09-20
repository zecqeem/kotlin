package org.example.ast;

public class ReturnStmt implements Stmt {
    public final Expr expr;
    public ReturnStmt(Expr expr) { this.expr = expr;}

    @Override
    public String toString() {
        return "ReturnStmt{" +
                "expr=" + expr +
                '}';
    }
}
