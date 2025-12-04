package org.example.ast;

public class StringLiteral implements Expr {
    public final String value;
    public final int line;
    @Override
    public String toString() {
        return "StringLiteral{" +
                "value='" + value + '\'' +
                '}';
    }

    public StringLiteral(String value,int line) {
        this.value = value;
        this.line = line;
    }
}