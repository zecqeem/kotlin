package org.example.ast;

public class AssignStmt implements Stmt {
    public final String name;
    public final Expr value;
    public final int line;

    @Override
    public String toString() {
        return "AssignStmt{" +
                "name='" + name + '\'' +
                ", value=" + value +
                '}';
    }

    public AssignStmt(String name, Expr value, int line) {
        this.line = line;
        this.name = name;
        this.value = value;
    }
}