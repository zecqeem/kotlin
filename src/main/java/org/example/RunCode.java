package org.example;

import org.example.ast.Stmt;

import java.lang.reflect.Method;
import java.util.List;

public class RunCode {
    public void running (String code) throws Exception{
        Lexer lexer = new Lexer(code);
        Parser parser = new Parser(lexer);
        List<Stmt> program = parser.parse();

        CodeGenerator gen = new CodeGenerator(program);
        Class<?> cls = gen.compileAndLoad();

        Method m = cls.getMethod("main", String[].class);
        String[] arr = new String[0];
        m.invoke(null, (Object) arr);
    }
}
