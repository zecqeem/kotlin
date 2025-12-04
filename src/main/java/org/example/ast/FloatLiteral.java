package org.example.ast;

public class FloatLiteral implements Expr {
    public final double value;
    public final int line;
    public FloatLiteral(double value,int line) { this.value = value;this.line = line; }

    @Override
    public String toString() {
        return "FloatLiteral{" +
                "value=" + value +
                '}';
    }
}

