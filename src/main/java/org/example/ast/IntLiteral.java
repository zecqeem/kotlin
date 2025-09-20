package org.example.ast;


public class IntLiteral implements Expr {
    public final int value;
    public IntLiteral(int value) { this.value = value; }

    @Override
    public String toString() {
        return "IntLiteral{" +
                "value=" + value +
                '}';
    }
}
