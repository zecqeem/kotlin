package org.example.ast;


public class IntLiteral implements Expr {
    public final int value;
    public final int line;
    public IntLiteral(int value,int line) { this.value = value;this.line = line; }

    @Override
    public String toString() {
        return "IntLiteral{" +
                "value=" + value +
                '}';
    }
}
