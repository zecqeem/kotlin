package org.example.ast;

public class BoolLiteral implements Expr {
    public final boolean value;
    public final int line;
    public BoolLiteral(boolean value,int line) { this.value = value;this.line = line; }

    @Override
    public String toString() {
        return "BoolLiteral{" +
                "value=" + value +
                '}';
    }
}

