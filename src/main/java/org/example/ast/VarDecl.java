package org.example.ast;

public class VarDecl implements Stmt {
    public final String name;
    public final String type; // float, int, string, bool
    public final Expr value;
    public VarDecl(String name, String type, Expr value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        return "VarDecl{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", value=" + value +
                '}';
    }
}
