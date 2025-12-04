package org.example.ast;

import java.util.List;

public class CallExpr implements Expr {
    public final String name;
    public final List<Expr> args;
    public final int line;
    public CallExpr(String name, List<Expr> args,int line) {
        this.name = name;
        this.args = args;
        this.line = line;
    }

    @Override
    public String toString() {
        return "Call(" + name + ", args=" + args + ")";
    }
}