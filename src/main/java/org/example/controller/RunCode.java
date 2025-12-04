package org.example;

import org.example.ast.Stmt;
import org.example.tokens.Token;
import org.example.tokens.TokenType;

import java.lang.reflect.Method;
import java.util.List;

public class RunCode {
    public void running (String code) throws Exception{
        Lexer lexer1 = new Lexer(code);
        System.out.println(lexer1.tokenize());
    }
}
