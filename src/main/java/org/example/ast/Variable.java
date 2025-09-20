package org.example.ast;

public class Variable implements Expr {
    public final String name;
    public Variable(String name) { this.name = name; }

    @Override
    public String toString() {
        return "Variable{" +
                "name='" + name + '\'' +
                '}';
    }
}
