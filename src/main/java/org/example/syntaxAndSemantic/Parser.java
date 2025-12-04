package org.example.syntaxAndSemantic;

import org.example.ast.*;
import org.example.tokens.Token;
import org.example.tokens.TokenType;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // Helpers

    private Token peek() { return tokens.get(current); }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw new RuntimeException(message + " at line " + peek().line);
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return tokens.get(current - 1);
    }

    private boolean isAtEnd() { return peek().type == TokenType.EOF; }

    //Parsing Logic

    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(statement());
        }
        return statements;
    }

    private Stmt statement() {
        if (check(TokenType.CONST)) return constDecl();
        if (check(TokenType.VAR)) return varDecl();
        if (check(TokenType.FUN)) return funDecl();
        if (check(TokenType.IF)) return ifStmt();
        if (check(TokenType.WHILE)) return whileStmt();
        if (check(TokenType.PRINT)) return printStmt();
        if (check(TokenType.INPUT)) return inputStmt();
        if (check(TokenType.RETURN)) return returnStmt();
        if (check(TokenType.LBRACE)) return block();
        return assignOrExprStmt();
    }

    // --- Declarations ---

    private Stmt constDecl() {
        Token keyword = consume(TokenType.CONST, "Expected 'const'");
        String name = consume(TokenType.IDENTIFIER, "Expected const name").text;
        String type = null;
        if (check(TokenType.COLON)) {
            consume(TokenType.COLON, "Expected ':'");
            type = parseType();
        }
        consume(TokenType.ASSIGN, "Expected '='");
        Expr value = expression();
        // Pass keyword.line
        return new VarDecl(name, type, value, true, keyword.line);
    }

    private Stmt varDecl() {
        Token keyword = consume(TokenType.VAR, "Expected 'var'");
        String name = consume(TokenType.IDENTIFIER, "Expected variable name").text;
        consume(TokenType.COLON, "Expected ':'");
        String type = parseType();
        consume(TokenType.ASSIGN, "Expected '='");
        Expr value = expression();
        return new VarDecl(name, type, value, false, keyword.line);
    }

    private Stmt funDecl() {
        Token keyword = consume(TokenType.FUN, "Expected 'fun'");
        String name = consume(TokenType.IDENTIFIER, "Expected function name").text;
        consume(TokenType.LPAREN, "Expected '('");
        List<VarDecl> params = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                if (params.size() > 0) consume(TokenType.COMMA, "Expected ','");
                Token paramNameToken = consume(TokenType.IDENTIFIER, "Expected param name");
                consume(TokenType.COLON, "Expected ':'");
                String paramType = parseType();
                // Parameters are also VarDecls
                params.add(new VarDecl(paramNameToken.text, paramType, null, false, paramNameToken.line));
            } while (check(TokenType.COMMA));
        }
        consume(TokenType.RPAREN, "Expected ')'");
        consume(TokenType.ARROW, "Expected '->'");
        String returnType = parseType();
        Stmt body = block(); // block() returns BlockStmt
        return new FunDecl(name, returnType, params, (BlockStmt) body, keyword.line);
    }

    // --- Statements ---

    private Stmt ifStmt() {
        Token keyword = consume(TokenType.IF, "Expected 'if'");
        consume(TokenType.LPAREN, "Expected '('");
        Expr condition = expression();
        consume(TokenType.RPAREN, "Expected ')'");
        Stmt thenBranch = block();
        Stmt elseBranch = null;
        if (check(TokenType.ELSE)) {
            consume(TokenType.ELSE, "Expected 'else'");
            elseBranch = check(TokenType.IF) ? ifStmt() : block();
        }
        return new IfStmt(condition, thenBranch, elseBranch, keyword.line);
    }

    private Stmt whileStmt() {
        Token keyword = consume(TokenType.WHILE, "Expected 'while'");
        consume(TokenType.LPAREN, "Expected '('");
        Expr condition = expression();
        consume(TokenType.RPAREN, "Expected ')'");
        Stmt body = block();
        return new WhileStmt(condition, body, keyword.line);
    }

    private Stmt block() {
        Token brace = consume(TokenType.LBRACE, "Expected '{'");
        List<Stmt> statements = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            statements.add(statement());
        }
        consume(TokenType.RBRACE, "Expected '}'");
        return new BlockStmt(statements, brace.line);
    }

    private Stmt printStmt() {
        Token keyword = consume(TokenType.PRINT, "Expected 'print'");
        consume(TokenType.LPAREN, "Expected '('");
        Expr expr = expression();
        consume(TokenType.RPAREN, "Expected ')'");
        return new PrintStmt(expr, keyword.line);
    }

    private Stmt inputStmt() {
        Token keyword = consume(TokenType.INPUT, "Expected 'input'");
        consume(TokenType.LPAREN, "Expected '('");
        String name = consume(TokenType.IDENTIFIER, "Expected var name").text;
        consume(TokenType.RPAREN, "Expected ')'");
        return new InputStmt(name, keyword.line);
    }

    private Stmt returnStmt() {
        Token keyword = consume(TokenType.RETURN, "Expected 'return'");
        Expr value = expression();
        return new ReturnStmt(value, keyword.line);
    }

    private Stmt assignOrExprStmt() {
        // Capture line before parsing expression just in case
        int line = peek().line;
        Expr expr = expression();

        if (check(TokenType.ASSIGN)) {
            Token equals = consume(TokenType.ASSIGN, "Expected '='");
            if (expr instanceof Variable) {
                String name = ((Variable) expr).name;
                Expr value = expression();
                return new AssignStmt(name, value, equals.line);
            }
            throw new RuntimeException("Invalid assignment target at line " + equals.line);
        }
        return new ExprStmt(expr, line);
    }

    //Expressions

    private Expr expression() { return equality(); }

    private Expr equality() {
        Expr expr = comparison();
        while (check(TokenType.EQ) || check(TokenType.NEQ)) {
            Token op = advance();
            Expr right = comparison();
            expr = new BinaryExpr(expr, op.text, right, op.line);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (check(TokenType.GT) || check(TokenType.GE) ||
                check(TokenType.LT) || check(TokenType.LE)) {
            Token op = advance();
            Expr right = term();
            expr = new BinaryExpr(expr, op.text, right, op.line);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            Token op = advance();
            Expr right = factor();
            expr = new BinaryExpr(expr, op.text, right, op.line);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = power();
        while (check(TokenType.STAR) || check(TokenType.SLASH)) {
            Token op = advance();
            Expr right = power();
            expr = new BinaryExpr(expr, op.text, right, op.line);
        }
        return expr;
    }

    private Expr power() {
        Expr left = unary();
        if (check(TokenType.CARET)) {
            Token op = advance();
            Expr right = power(); // Right associative
            return new BinaryExpr(left, op.text, right, op.line);
        }
        return left;
    }

    private Expr unary() {
        if (check(TokenType.NOT) || check(TokenType.MINUS)) {
            Token op = advance();
            Expr right = unary();
            if (op.text.equals("-")) {
                // Synthetic "0" for unary minus gets the line of the minus sign
                return new BinaryExpr(new IntLiteral(0, op.line), "-", right, op.line);
            }
            // For NOT, handle accordingly or treat as primary
        }
        return primary();
    }

    private Expr primary() {
        if (check(TokenType.FALSE)) {
            Token t = advance();
            return new BoolLiteral(false, t.line);
        }
        if (check(TokenType.TRUE)) {
            Token t = advance();
            return new BoolLiteral(true, t.line);
        }
        if (check(TokenType.INT)) {
            Token t = advance();
            return new IntLiteral(Integer.parseInt(t.text), t.line);
        }
        if (check(TokenType.FLOAT)) {
            Token t = advance();
            return new FloatLiteral(Double.parseDouble(t.text), t.line);
        }
        if (check(TokenType.STRING)) {
            Token t = advance();
            String val = t.text;
            if (val.startsWith("\"")) val = val.substring(1, val.length() - 1);
            return new StringLiteral(val, t.line);
        }
        if (check(TokenType.IDENTIFIER)) {
            Token nameToken = advance();
            if (check(TokenType.LPAREN)) {
                consume(TokenType.LPAREN, "Expected '('");
                List<Expr> args = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    do {
                        if (args.size() > 0) consume(TokenType.COMMA, "Expected ','");
                        args.add(expression());
                    } while (check(TokenType.COMMA));
                }
                consume(TokenType.RPAREN, "Expected ')'");
                return new CallExpr(nameToken.text, args, nameToken.line);
            }
            return new Variable(nameToken.text, nameToken.line);
        }
        if (check(TokenType.LPAREN)) {
            advance();
            Expr expr = expression();
            consume(TokenType.RPAREN, "Expected ')'");
            return expr;
        }
        throw new RuntimeException("Unexpected token at line " + peek().line);
    }

    private String parseType() {
        if (check(TokenType.INT_TYPE)) return consume(TokenType.INT_TYPE, "").text;
        if (check(TokenType.FLOAT_TYPE)) return consume(TokenType.FLOAT_TYPE, "").text;
        if (check(TokenType.BOOL_TYPE)) return consume(TokenType.BOOL_TYPE, "").text;
        if (check(TokenType.STRING_TYPE)) return consume(TokenType.STRING_TYPE, "").text;
        throw new RuntimeException("Expected type at line " + peek().line);
    }
}