package org.example.ast;

public class VarDecl implements Stmt {
    public final String name;
    public final String type;      // Может быть null, если это const (тип выводится)
    public final Expr initializer; // Может быть null (например, в аргументах функции)
    public final boolean isConstant; // Новое поле
    public final int line;
    public VarDecl(String name, String type, Expr initializer, boolean isConstant,int line) {
        this.name = name;
        this.type = type;
        this.initializer = initializer;
        this.isConstant = isConstant;
        this.line = line;
    }

    @Override
    public String toString() {
        return "VarDecl{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", initializer=" + initializer +
                ", isConstant=" + isConstant +
                '}';
    }
}