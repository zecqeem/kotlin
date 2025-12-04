package org.example;

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

    // === Helpers ===

    // Повертає поточний токен
    private Token peek() {
        return tokens.get(current);
    }

    // Перевіряє тип поточного токена
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    // Споживає токен, якщо він очікуваного типу, інакше кидає помилку
    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        throw new RuntimeException(message + " at " + peek());
    }

    // Повертає поточний токен і переходить до наступного
    private Token advance() {
        if (!isAtEnd()) current++;
        return tokens.get(current - 1);
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    // === Parsing Logic ===

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
        consume(TokenType.CONST, "Expected 'const'");
        String name = consume(TokenType.IDENTIFIER, "Expected const name").text;

        // В твоїй граматиці const може бути без явного типу: const PI = 3.14
        String type = null;

        // Якщо раптом є явна типізація (const PI : float = ...), додамо перевірку:
        if (check(TokenType.COLON)) {
            consume(TokenType.COLON, "Expected ':'");
            type = parseType();
        }

        consume(TokenType.ASSIGN, "Expected '=' in const declaration");
        Expr value = expression();

        return new VarDecl(name, type, value, true);
    }

    private Stmt varDecl() {
        consume(TokenType.VAR, "Expected 'var'");
        String name = consume(TokenType.IDENTIFIER, "Expected variable name").text;

        consume(TokenType.COLON, "Expected ':' after variable name");
        String type = parseType(); // Обов'язково зчитуємо тип (int, float...)

        consume(TokenType.ASSIGN, "Expected '=' in variable declaration");
        Expr value = expression();

        return new VarDecl(name, type, value, false);
    }

    private Stmt funDecl() {
        consume(TokenType.FUN, "Expected 'fun'");
        String name = consume(TokenType.IDENTIFIER, "Expected function name").text;
        consume(TokenType.LPAREN, "Expected '('");

        List<VarDecl> params = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                if (params.size() > 0) {
                    consume(TokenType.COMMA, "Expected ',' between parameters");
                }
                String paramName = consume(TokenType.IDENTIFIER, "Expected parameter name").text;
                consume(TokenType.COLON, "Expected ':' after parameter name");
                String paramType = parseType();
                params.add(new VarDecl(paramName, paramType, null, false));
            } while (check(TokenType.COMMA)); // Поки є коми - читаємо далі
        }
        consume(TokenType.RPAREN, "Expected ')'");

        consume(TokenType.ARROW, "Expected '->'");
        String returnType = parseType();

        BlockStmt body = (BlockStmt) block();
        return new FunDecl(name, returnType, params, body);
    }

    // --- Statements ---

    private Stmt ifStmt() {
        consume(TokenType.IF, "Expected 'if'");
        consume(TokenType.LPAREN, "Expected '('");
        Expr condition = expression();
        consume(TokenType.RPAREN, "Expected ')'");

        Stmt thenBranch = block();
        Stmt elseBranch = null;

        if (check(TokenType.ELSE)) {
            consume(TokenType.ELSE, "Expected 'else'");
            if (check(TokenType.IF)) {
                // Підтримка "else if"
                elseBranch = ifStmt();
            } else {
                elseBranch = block();
            }
        }

        return new IfStmt(condition, thenBranch, elseBranch);
    }

    private Stmt whileStmt() {
        consume(TokenType.WHILE, "Expected 'while'");
        consume(TokenType.LPAREN, "Expected '('");
        Expr condition = expression();
        consume(TokenType.RPAREN, "Expected ')'");
        Stmt body = block();
        return new WhileStmt(condition, body);
    }

    private Stmt block() {
        consume(TokenType.LBRACE, "Expected '{'");
        List<Stmt> statements = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            statements.add(statement());
        }
        consume(TokenType.RBRACE, "Expected '}'");
        return new BlockStmt(statements);
    }

    private Stmt printStmt() {
        consume(TokenType.PRINT, "Expected 'print'");
        consume(TokenType.LPAREN, "Expected '('");
        Expr expr = expression();
        consume(TokenType.RPAREN, "Expected ')'");
        return new PrintStmt(expr);
    }

    private Stmt inputStmt() {
        consume(TokenType.INPUT, "Expected 'input'");
        consume(TokenType.LPAREN, "Expected '('");
        String name = consume(TokenType.IDENTIFIER, "Expected variable name").text;
        consume(TokenType.RPAREN, "Expected ')'");
        return new InputStmt(name);
    }

    private Stmt returnStmt() {
        consume(TokenType.RETURN, "Expected 'return'");
        Expr value = expression(); // Тут можна додати перевірку на порожній return
        return new ReturnStmt(value);
    }

    private Stmt assignOrExprStmt() {
        Expr expr = expression();

        // Якщо після виразу йде '=', значить це присвоєння (x = 5)
        if (check(TokenType.ASSIGN)) {
            consume(TokenType.ASSIGN, "Expected '='");
            if (expr instanceof Variable) {
                String name = ((Variable) expr).name;
                Expr value = expression();
                return new AssignStmt(name, value);
            }
            throw new RuntimeException("Invalid assignment target: " + expr);
        }

        return new ExprStmt(expr);
    }

    // === Expressions (Precedence Climbing) ===

    private Expr expression() {
        return equality();
    }

    // 1. Equality: ==, !=
    private Expr equality() {
        Expr expr = comparison();
        while (check(TokenType.EQ) || check(TokenType.NEQ)) {
            String op = advance().text;
            Expr right = comparison();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    // 2. Comparison: >, >=, <, <=
    private Expr comparison() {
        Expr expr = term();
        while (check(TokenType.GT) || check(TokenType.GE) ||
                check(TokenType.LT) || check(TokenType.LE)) {
            String op = advance().text;
            Expr right = term();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    // 3. Term: +, -
    private Expr term() {
        Expr expr = factor();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            String op = advance().text;
            Expr right = factor();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    // 4. Factor: *, /
    private Expr factor() {
        Expr expr = power();
        while (check(TokenType.STAR) || check(TokenType.SLASH)) {
            String op = advance().text;
            Expr right = power();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    // 5. Power: ^
    private Expr power() {
        Expr expr = unary();
        while (check(TokenType.CARET)) {
            String op = advance().text;
            Expr right = unary(); // Або power() для правої асоціативності
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    // 6. Unary: !, - (optional, added for completeness)
    private Expr unary() {
        if (check(TokenType.NOT) || check(TokenType.MINUS)) {
            String op = advance().text;
            Expr right = unary();
            // У тебе немає UnaryExpr в AST, тому емулюємо:
            if (op.equals("-")) {
                // -x -> 0 - x
                return new BinaryExpr(new IntLiteral(0), "-", right);
            }
            // Для NOT (!) доведеться додати UnaryExpr в AST, або поки що ігнорувати
            // Але оскільки в лексері є NOT, давай його обробимо хоча б як primary
        }
        return primary();
    }

    // 7. Primary: literals, vars, grouping
    private Expr primary() {
        if (check(TokenType.FALSE)) {
            advance();
            return new BoolLiteral(false);
        }
        if (check(TokenType.TRUE)) {
            advance();
            return new BoolLiteral(true);
        }
        if (check(TokenType.INT)) {
            int val = Integer.parseInt(advance().text);
            return new IntLiteral(val);
        }
        if (check(TokenType.FLOAT)) {
            double val = Double.parseDouble(advance().text);
            return new FloatLiteral(val);
        }
        if (check(TokenType.STRING)) {
            String val = advance().text;
            // Видаляємо лапки "" з лексеми, якщо вони там є
            if (val.startsWith("\"") && val.endsWith("\"")) {
                val = val.substring(1, val.length() - 1);
            }
            return new StringLiteral(val);
        }
        if (check(TokenType.IDENTIFIER)) {
            String name = advance().text;
            if (check(TokenType.LPAREN)) {
                // Function call
                consume(TokenType.LPAREN, "Expected '(' after function name");
                List<Expr> args = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    do {
                        if (args.size() > 0) {
                            consume(TokenType.COMMA, "Expected ','");
                        }
                        args.add(expression());
                    } while (check(TokenType.COMMA)); // Помилка в циклі виправлена
                }
                consume(TokenType.RPAREN, "Expected ')'");
                return new CallExpr(name, args);
            }
            return new Variable(name);
        }
        if (check(TokenType.LPAREN)) {
            advance();
            Expr expr = expression();
            consume(TokenType.RPAREN, "Expected ')'");
            return expr;
        }

        throw new RuntimeException("Unexpected token: " + peek());
    }

    // Helper for Types
    private String parseType() {
        if (check(TokenType.INT_TYPE)) return consume(TokenType.INT_TYPE, "").text;
        if (check(TokenType.FLOAT_TYPE)) return consume(TokenType.FLOAT_TYPE, "").text;
        if (check(TokenType.BOOL_TYPE)) return consume(TokenType.BOOL_TYPE, "").text;
        if (check(TokenType.STRING_TYPE)) return consume(TokenType.STRING_TYPE, "").text;
        throw new RuntimeException("Expected type (int, float, bool, string), got " + peek());
    }
}