package org.example.ast;

import java.util.List;

public class CallExpr implements Expr {
    public final String name;
    public final List<Expr> args;

    public CallExpr(String name, List<Expr> args) {
        this.name = name;
        this.args = args;
    }

    @Override
    public String toString() {
        return "Call(" + name + ", args=" + args + ")";
    }
}