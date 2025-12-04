package org.example.controller;

import org.example.ast.*;
import java.util.List;

public class AstPrinter {

    private static final String CROSS   = "├── ";
    private static final String CORNER  = "└── ";
    private static final String VERTICAL = "│   ";
    private static final String SPACE    = "    ";

    public void print(List<Stmt> statements) {
        System.out.println("PROGRAM ROOT");
        for (int i = 0; i < statements.size(); i++) {
            printStmt(statements.get(i), "", i == statements.size() - 1);
        }
    }

    private void printStmt(Stmt stmt, String prefix, boolean isLast) {
        String connector = isLast ? CORNER : CROSS;
        String childPrefix = prefix + (isLast ? SPACE : VERTICAL);

        System.out.print(prefix + connector);

        if (stmt instanceof VarDecl) {
            VarDecl v = (VarDecl) stmt;
            String kind = v.isConstant ? "Const" : "Var";
            System.out.println(kind + "Decl: " + v.name + (v.type != null ? " (" + v.type + ")" : ""));
            if (v.initializer != null) {
                printExpr(v.initializer, childPrefix, true, "init");
            }

        } else if (stmt instanceof FunDecl) {
            FunDecl f = (FunDecl) stmt;
            System.out.println("FunDecl: " + f.name + " -> " + f.returnType);

            if (!f.params.isEmpty()) {
                System.out.println(childPrefix + CROSS + "Params:");
                String paramPrefix = childPrefix + VERTICAL;
                for (int i = 0; i < f.params.size(); i++) {
                    VarDecl p = f.params.get(i);
                    boolean lastParam = (i == f.params.size() - 1);
                    System.out.println(paramPrefix + (lastParam ? CORNER : CROSS) + p.name + ": " + p.type);
                }
            }
            printStmt(f.body, childPrefix, true);

        } else if (stmt instanceof BlockStmt) {
            System.out.println("Block");
            List<Stmt> stmts = ((BlockStmt) stmt).statements;
            for (int i = 0; i < stmts.size(); i++) {
                printStmt(stmts.get(i), childPrefix, i == stmts.size() - 1);
            }

        } else if (stmt instanceof IfStmt) {
            IfStmt i = (IfStmt) stmt;
            System.out.println("If");
            boolean hasElse = i.elseBranch != null;

            printExpr(i.condition, childPrefix, false, "Cond");

            System.out.println(childPrefix + (hasElse ? CROSS : CORNER) + "Then:");
            printStmt(i.thenBranch, childPrefix + (hasElse ? VERTICAL : SPACE), true);

            if (hasElse) {
                System.out.println(childPrefix + CORNER + "Else:");
                printStmt(i.elseBranch, childPrefix + SPACE, true);
            }

        } else if (stmt instanceof WhileStmt) {
            WhileStmt w = (WhileStmt) stmt;
            System.out.println("While");
            printExpr(w.condition, childPrefix, false, "Cond");
            System.out.println(childPrefix + CORNER + "Do:");
            printStmt(w.body, childPrefix + SPACE, true);

        } else if (stmt instanceof PrintStmt) {
            System.out.println("Print");
            printExpr(((PrintStmt) stmt).expr, childPrefix, true, null);

        } else if (stmt instanceof ReturnStmt) {
            System.out.println("Return");
            printExpr(((ReturnStmt) stmt).expr, childPrefix, true, null);

        } else if (stmt instanceof AssignStmt) {
            AssignStmt a = (AssignStmt) stmt;
            System.out.println("Assign: " + a.name);
            printExpr(a.value, childPrefix, true, "value");

        } else if (stmt instanceof InputStmt) {
            System.out.println("Input: " + ((InputStmt) stmt).variableName);

        } else if (stmt instanceof ExprStmt) {
            System.out.println("ExprStmt");
            printExpr(((ExprStmt) stmt).expr, childPrefix, true, null);
        }
    }

    private void printExpr(Expr expr, String prefix, boolean isLast, String label) {
        String connector = isLast ? CORNER : CROSS;
        String childPrefix = prefix + (isLast ? SPACE : VERTICAL);

        System.out.print(prefix + connector);
        if (label != null) System.out.print(label + ": ");

        if (expr instanceof BinaryExpr) {
            BinaryExpr b = (BinaryExpr) expr;
            System.out.println("BinaryOp (" + b.op + ")");
            printExpr(b.left, childPrefix, false, "L");
            printExpr(b.right, childPrefix, true, "R");

        } else if (expr instanceof CallExpr) {
            CallExpr c = (CallExpr) expr;
            System.out.println("Call " + c.name + "()");
            for (int i = 0; i < c.args.size(); i++) {
                printExpr(c.args.get(i), childPrefix, i == c.args.size() - 1, "Arg" + i);
            }

        } else if (expr instanceof Variable) {
            System.out.println("Var(" + ((Variable) expr).name + ")");

        } else if (expr instanceof IntLiteral) {
            System.out.println(((IntLiteral) expr).value);

        } else if (expr instanceof FloatLiteral) {
            System.out.println(((FloatLiteral) expr).value);

        } else if (expr instanceof StringLiteral) {
            System.out.println("\"" + ((StringLiteral) expr).value + "\"");

        } else if (expr instanceof BoolLiteral) {
            System.out.println(((BoolLiteral) expr).value);
        }
    }
}