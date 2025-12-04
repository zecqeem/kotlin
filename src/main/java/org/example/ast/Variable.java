package org.example.ast;

public class Variable implements Expr {
    public final String name;
    public final int line;
    public Variable(String name,int line) { this.name = name;this.line = line; }

    @Override
    public String toString() {
        return "Variable{" +
                "name='" + name + '\'' +
                '}';
    }
}
