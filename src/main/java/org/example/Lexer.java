package org.example;
import org.example.tokens.Token;
import org.example.tokens.TokenType;

import java.util.HashMap;
import java.util.Map;

public class Lexer {
    private final String input;
    private int pos = 0;

    private static final Map<String, TokenType> keywords = new HashMap<>();
    static {
        keywords.put("if", TokenType.IF);
        keywords.put("else", TokenType.ELSE);
        keywords.put("while", TokenType.WHILE);
        keywords.put("fun", TokenType.FUN);
        keywords.put("return", TokenType.RETURN);
        keywords.put("const", TokenType.CONST);
        keywords.put("true", TokenType.TRUE);
        keywords.put("false", TokenType.FALSE);
        keywords.put("input", TokenType.INPUT);
        keywords.put("print", TokenType.PRINT);
        keywords.put("float", TokenType.FLOAT_TYPE);
        keywords.put("int", TokenType.INT_TYPE);
        keywords.put("bool", TokenType.BOOL_TYPE);
        keywords.put("string", TokenType.STRING_TYPE);
    }

    public Lexer(String input) {
        this.input = input;
    }

    public Token nextToken() {
        skipWhitespaceAndComments();

        if (pos >= input.length()) return new Token(TokenType.EOF, "");

        char ch = input.charAt(pos);

        // number
        if (Character.isDigit(ch)) return number();

        // isents / keywords
        if (Character.isLetter(ch)) return identifier();

        // strings
        if (ch == '"') return string();

        switch (ch) {
            case '+': return token(TokenType.PLUS);
            case '-':
                if (peekNext() == '>') {
                    pos += 2;
                    return new Token(TokenType.ARROW, "->");
                }
                pos++;
                return new Token(TokenType.MINUS, "-");
            case '*': return token(TokenType.STAR);
            case '/': return token(TokenType.SLASH);
            case '^': return token(TokenType.CARET);
            case ':': return token(TokenType.COLON);
            case ',': return token(TokenType.COMMA);
            case '=':
                if (peekNext() != null && peekNext() == '=') {
                    pos += 2;
                    return new Token(TokenType.EQ, "==");
                }
                pos++;
                return new Token(TokenType.ASSIGN, "=");
            case '!':
                if (peekNext() != null && peekNext() == '=') {
                    pos += 2;
                    return new Token(TokenType.NEQ, "!=");
                }
                pos++;
                throw error("Unexpected '!'");
            case '<':
                if (peekNext() != null && peekNext() == '=') {
                    pos += 2;
                    return new Token(TokenType.LE, "<=");
                }
                pos++;
                return new Token(TokenType.LT, "<");
            case '>':
                if (peekNext() != null && peekNext() == '=') {
                    pos += 2;
                    return new Token(TokenType.GE, ">=");
                }
                pos++;
                return new Token(TokenType.GT, ">");
            case '(' : return token(TokenType.LPAREN);
            case ')' : return token(TokenType.RPAREN);
            case '{' : return token(TokenType.LBRACE);
            case '}' : return token(TokenType.RBRACE);
            default: throw error("Unknown character: " + ch);
        }
    }

    private Token number() {
        int start = pos;
        while (pos < input.length() &&
                (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) pos++;
        String text = input.substring(start, pos);
        if (text.contains(".")) return new Token(TokenType.FLOAT, text);
        else return new Token(TokenType.INT, text);
    }

    private Token identifier() {
        int start = pos;
        while (pos < input.length() &&
                (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_')) pos++;
        String text = input.substring(start, pos);
        TokenType type = keywords.getOrDefault(text, TokenType.IDENTIFIER);
        return new Token(type, text);
    }

    private Token string() {
        pos++; // skip opening "
        int start = pos;
        while (pos < input.length() && input.charAt(pos) != '"') pos++;
        String text = input.substring(start, pos);
        pos++; // skip closing "
        return new Token(TokenType.STRING, text);
    }

    private Token token(TokenType type) {
        return new Token(type, String.valueOf(input.charAt(pos++)));
    }

    private void skipWhitespaceAndComments() {
        while (pos < input.length()) {
            char ch = input.charAt(pos);
            if (Character.isWhitespace(ch)) {
                pos++;
            } else if (ch == '/' && peekNext() != null && peekNext() == '/') {
                while (pos < input.length() && input.charAt(pos) != '\n') pos++;
            } else break;
        }
    }

    private Character peekNext() {
        return (pos + 1 < input.length()) ? input.charAt(pos + 1) : null;
    }

    private RuntimeException error(String msg) {
        return new RuntimeException("Lexer error at pos " + pos + ": " + msg);
    }
}