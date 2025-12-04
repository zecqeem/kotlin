package org.example.ast;

public class ReturnStmt implements Stmt {
    public final Expr expr;
    public final int line;
    public ReturnStmt(Expr expr,int line) { this.expr = expr;this.line = line;}

    @Override
    public String toString() {
        return "ReturnStmt{" +
                "expr=" + expr +
                '}';
    }
}
