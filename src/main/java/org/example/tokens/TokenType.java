package org.example.tokens;

public enum TokenType {
    // Ключевые слова
    IF, ELSE, WHILE, FUN, RETURN, CONST, TRUE, FALSE, INPUT, PRINT, VAR,

    // Идентификаторы и литералы
    IDENTIFIER, INT, FLOAT, STRING, BOOL,
    FLOAT_TYPE, INT_TYPE, STRING_TYPE, BOOL_TYPE,

    // Операторы
    PLUS, MINUS, STAR, SLASH, CARET,ARROW,
    EQ, NEQ, LT, GT, LE, GE, ASSIGN, COLON,COMMA,NOT,

    // Скобки
    LPAREN, RPAREN, LBRACE, RBRACE,

    // Конец файла
    EOF
}
