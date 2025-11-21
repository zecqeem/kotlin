package org.example.ast;

public class BoolLiteral implements Expr {
    public final boolean value;
    public BoolLiteral(boolean value) { this.value = value; }

    @Override
    public String toString() {
        return "BoolLiteral{" +
                "value=" + value +
                '}';
    }
}

