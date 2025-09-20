package org.example.ast;


public class PrintStmt implements Stmt {
    public final Expr expr;
    public PrintStmt(Expr expr) { this.expr = expr; }

    @Override
    public String toString() {
        return "PrintStmt{" +
                "expr=" + expr +
                '}';
    }
}
