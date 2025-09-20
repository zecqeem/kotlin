package org.example.ast;

public class FloatLiteral implements Expr {
    public final double value;
    public FloatLiteral(double value) { this.value = value; }

    @Override
    public String toString() {
        return "FloatLiteral{" +
                "value=" + value +
                '}';
    }
}

