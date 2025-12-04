package org.example.ast;

public class InputStmt implements Stmt {
    public final String variableName;
    public final int line;

    public InputStmt(String variableName,int line) {
        this.variableName = variableName;
        this.line = line;
    }

    @Override
    public String toString() {
        return "InputStmt{" +
                "variableName='" + variableName + '\'' +
                '}';
    }
}