package org.example.controller;

import org.example.GorbBaseVisitor;
import org.example.GorbParser;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class GorbEvaluator extends GorbBaseVisitor<Object> {

    private final Map<String, Object> variables = new HashMap<>();
    private final Scanner scanner = new Scanner(System.in);

    @Override
    public Object visitVarDecl(GorbParser.VarDeclContext ctx) {
        String varName = ctx.ID().getText();
        Object value = null;

        if (ctx.expression() != null) {
            value = visit(ctx.expression());
        }

        // Dynamic typing: storing value without strict type checking
        variables.put(varName, value);
        return value;
    }

    @Override
    public Object visitAssignStmt(GorbParser.AssignStmtContext ctx) {
        String varName = ctx.ID().getText();

        if (!variables.containsKey(varName)) {
            throw new RuntimeException("Error: Variable '" + varName + "' was not declared via var.");
        }

        Object value = visit(ctx.expression());
        variables.put(varName, value);
        return value;
    }

    @Override
    public Object visitPrintStmt(GorbParser.PrintStmtContext ctx) {
        Object value = visit(ctx.expression());
        System.out.println(value);
        return value;
    }

    @Override
    public Object visitInputStmt(GorbParser.InputStmtContext ctx) {
        String varName = ctx.ID().getText();
        System.out.print("Enter value for " + varName + ": ");
        String input = scanner.nextLine();

        Object parsedValue;
        try {
            parsedValue = Integer.parseInt(input);
        } catch (NumberFormatException e1) {
            try {
                parsedValue = Double.parseDouble(input);
            } catch (NumberFormatException e2) {
                parsedValue = input; // Save as string if not a number
            }
        }

        variables.put(varName, parsedValue);
        return null;
    }

    @Override
    public Object visitIfStmt(GorbParser.IfStmtContext ctx) {
        Object condResult = visit(ctx.expression());

        if (Boolean.TRUE.equals(condResult)) {
            return visit(ctx.statement(0));
        } else if (ctx.statement().size() > 1) {
            return visit(ctx.statement(1)); // Else block
        }
        return null;
    }

    @Override
    public Object visitWhileStmt(GorbParser.WhileStmtContext ctx) {
        while (Boolean.TRUE.equals(visit(ctx.expression()))) {
            visit(ctx.statement());
        }
        return null;
    }

    @Override
    public Object visitAddSubExpr(GorbParser.AddSubExprContext ctx) {
        Object left = visit(ctx.multiDivExpr(0));

        for (int i = 1; i < ctx.multiDivExpr().size(); i++) {
            Object right = visit(ctx.multiDivExpr(i));

            // Calculate operator (+ or -). It is the child at index (2*i - 1)
            String op = ctx.getChild(2 * i - 1).getText();

            if (op.equals("+")) {
                // String concatenation
                if (left instanceof String || right instanceof String) {
                    left = left.toString() + right.toString();
                } else {
                    left = asDouble(left) + asDouble(right);
                }
            } else {
                left = asDouble(left) - asDouble(right);
            }
        }
        return formatNumber(left);
    }

    @Override
    public Object visitMultiDivExpr(GorbParser.MultiDivExprContext ctx) {
        Object left = visit(ctx.powerExpr(0));

        for (int i = 1; i < ctx.powerExpr().size(); i++) {
            Object right = visit(ctx.powerExpr(i));
            String op = ctx.getChild(2 * i - 1).getText();

            if (op.equals("*")) {
                left = asDouble(left) * asDouble(right);
            } else {
                double rVal = asDouble(right);
                if (rVal == 0) throw new ArithmeticException("Division by zero!");
                left = asDouble(left) / rVal;
            }
        }
        return formatNumber(left);
    }

    @Override
    public Object visitPowerExpr(GorbParser.PowerExprContext ctx) {
        Object left = visit(ctx.primaryExpr());

        // Grammar: primaryExpr (CARET powerExpr)?
        if (ctx.powerExpr() != null) {
            Object right = visit(ctx.powerExpr());
            double result = Math.pow(asDouble(left), asDouble(right));
            return formatNumber(result);
        }
        return left;
    }

    @Override
    public Object visitRelationalExpr(GorbParser.RelationalExprContext ctx) {
        Object left = visit(ctx.addSubExpr(0));

        if (ctx.addSubExpr().size() < 2) return left;

        Object right = visit(ctx.addSubExpr(1));
        String op = ctx.getChild(1).getText();

        double l = asDouble(left);
        double r = asDouble(right);

        switch (op) {
            case "<":  return l < r;
            case ">":  return l > r;
            case "<=": return l <= r;
            case ">=": return l >= r;
            case "==": return l == r;
            case "!=": return l != r;
            default:   return false;
        }
    }

    @Override
    public Object visitPrimaryExpr(GorbParser.PrimaryExprContext ctx) {
        if (ctx.INT() != null) return Integer.parseInt(ctx.INT().getText());
        if (ctx.FLOAT() != null) return Double.parseDouble(ctx.FLOAT().getText());
        if (ctx.BOOL() != null) return Boolean.parseBoolean(ctx.BOOL().getText());

        if (ctx.STRING() != null) {
            String s = ctx.STRING().getText();
            return s.substring(1, s.length() - 1); // Remove quotes
        }

        if (ctx.ID() != null) {
            String varName = ctx.ID().getText();
            if (variables.containsKey(varName)) {
                return variables.get(varName);
            } else {
                throw new RuntimeException("Error: variable '" + varName + "' not found.");
            }
        }

        // ( expression )
        if (ctx.expression() != null) {
            return visit(ctx.expression());
        }
        return null;
    }

    private Double asDouble(Object o) {
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        if (o instanceof Boolean) {
            return (Boolean) o ? 1.0 : 0.0;
        }
        if (o instanceof String) {
            try {
                return Double.parseDouble((String) o);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot use string \"" + o + "\" as a number");
            }
        }
        throw new RuntimeException("Cannot cast type " + o.getClass().getSimpleName() + " to number");
    }

    private Object formatNumber(Object o) {
        if (o instanceof Double) {
            Double d = (Double) o;
            // If fractional part is zero (e.g. 5.0)
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return d.intValue();
            }
        }
        return o;
    }
}