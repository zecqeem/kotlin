package org.example.ast;

public class BinaryExpr implements Expr {
    public final Expr left;
    public final String op;
    public final Expr right;
    public final int line;
    public BinaryExpr(Expr left, String op, Expr right,int line) {
        this.left = left;
        this.op = op;
        this.right = right;
        this.line = line;
    }

    @Override
    public String toString() {
        return "BinaryExpr{" +
                "left=" + left +
                ", op='" + op + '\'' +
                ", right=" + right +
                '}';
    }
}
