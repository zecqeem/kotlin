package org.example;

import org.example.ast.Stmt;
import org.example.tokens.Token;
import org.example.tokens.TokenType;

import java.lang.reflect.Method;
import java.util.List;

public class RunCode {
    public void running (String code) throws Exception{
        Lexer lexer1 = new Lexer(code);
        Token token;
        do {
            token = lexer1.nextToken();
            System.out.println(token.type + " : " + token.text);
        } while (token.type != TokenType.EOF);
        Lexer lexer = new Lexer(code);
        Parser parser = new Parser(lexer);
        List<Stmt> program = parser.parse();
        for (Stmt stmt : program){
            System.out.println(stmt.toString());
        }

        CodeGenerator gen = new CodeGenerator(program);
        Class<?> cls = gen.compileAndLoad();

        Method m = cls.getMethod("main", String[].class);
        String[] arr = new String[0];
        m.invoke(null, (Object) arr);
    }
}
