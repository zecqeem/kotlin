package org.example;

import org.example.ast.*;
import org.example.tokens.Token;
import org.example.tokens.TokenType;

import java.util.*;

public class Parser {
    private final Lexer lexer;
    private Token current;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        this.current = lexer.nextToken();
    }

    private void eat(TokenType type) {
        if (current.type == type) current = lexer.nextToken();
        else throw new RuntimeException("Unexpected token: " + current + ", expected " + type);
    }

    // Пример: parse программу (список statements)
    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (current.type != TokenType.EOF) {
            statements.add(statement());
        }
        return statements;
    }

    // Парсим statement
    private Stmt statement() {
        switch (current.type) {
            case CONST:
                return varDecl();
            case FUN:
                return funDecl();
            case PRINT:
                return printStmt();
            case RETURN:
                return returnStmt();
            default:
                return exprStmt();
        }
    }
    private Stmt returnStmt() {
        eat(TokenType.RETURN);
        Expr value = expression();
        return new ReturnStmt(value);
    }
    private Stmt varDecl() {
        eat(TokenType.CONST);
        String name = current.text;
        eat(TokenType.IDENTIFIER);
        eat(TokenType.ASSIGN);
        Expr value = expression();
        return new VarDecl(name, null, value); // тип можно добавить из текущего токена
    }

    private Stmt funDecl() {
        eat(TokenType.FUN);
        String name = current.text;
        eat(TokenType.IDENTIFIER);
        eat(TokenType.LPAREN);
        List<VarDecl> params = new ArrayList<>();
        if (current.type != TokenType.RPAREN) {
            params.add(param());
            while (current.type == TokenType.COMMA) {
                eat(TokenType.COMMA);
                params.add(param());
            }
        }
        eat(TokenType.RPAREN);
        eat(TokenType.ARROW);
        String returnType = current.text;
        eat(current.type); // FLOAT_TYPE, INT_TYPE, etc
        eat(TokenType.LBRACE);
        List<Stmt> body = new ArrayList<>();
        while (current.type != TokenType.RBRACE) {
            body.add(statement());
        }
        eat(TokenType.RBRACE);
        return new FunDecl(name, returnType, params, body);
    }

    private VarDecl param() {
        String name = current.text;
        eat(TokenType.IDENTIFIER);
        eat(TokenType.COLON);
        String type = current.text;
        eat(current.type);
        return new VarDecl(name, type, null);
    }

    private Stmt printStmt() {
        eat(TokenType.PRINT);
        eat(TokenType.LPAREN);
        Expr expr = expression();
        eat(TokenType.RPAREN);
        return new PrintStmt(expr);
    }

    private Stmt exprStmt() {
        Expr e = expression();
        return new ExprStmt(e);
    }

    // Минимальный парсер выражений
    private Expr expression() {
        Expr left = term();
        while (current.type == TokenType.PLUS || current.type == TokenType.MINUS) {
            String op = current.text;
            eat(current.type);
            left = new BinaryExpr(left, op, term());
        }
        return left;
    }

    private Expr term() {
        Expr left = factor();
        while (current.type == TokenType.STAR || current.type == TokenType.SLASH) {
            String op = current.text;
            eat(current.type);
            left = new BinaryExpr(left, op, factor());
        }
        return left;
    }

    private Expr factor() {
        Expr left;
        if (current.type == TokenType.INT) {
            left = new IntLiteral(Integer.parseInt(current.text));
            eat(TokenType.INT);
        } else if (current.type == TokenType.FLOAT) {
            left = new FloatLiteral(Double.parseDouble(current.text));
            eat(TokenType.FLOAT);
        } else if (current.type == TokenType.IDENTIFIER) {
            String name = current.text;
            eat(TokenType.IDENTIFIER);

            if (current.type == TokenType.LPAREN) {
                // Вызов функции
                eat(TokenType.LPAREN);
                List<Expr> args = new ArrayList<>();
                if (current.type != TokenType.RPAREN) {
                    args.add(expression());
                    while (current.type == TokenType.COMMA) {
                        eat(TokenType.COMMA);
                        args.add(expression());
                    }
                }
                eat(TokenType.RPAREN);
                left = new CallExpr(name, args);
            } else {
                // Просто переменная
                left = new Variable(name);
            }
        } else if (current.type == TokenType.LPAREN) {
            eat(TokenType.LPAREN);
            left = expression();
            eat(TokenType.RPAREN);
        } else {
            throw new RuntimeException("Unexpected token in factor: " + current);
        }
        return left;
    }
}