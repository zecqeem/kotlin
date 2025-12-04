package org.example.ast;


public class PrintStmt implements Stmt {
    public final Expr expr;
    public final int line;
    public PrintStmt(Expr expr,int line) { this.expr = expr;this.line = line; }

    @Override
    public String toString() {
        return "PrintStmt{" +
                "expr=" + expr +
                '}';
    }
}
