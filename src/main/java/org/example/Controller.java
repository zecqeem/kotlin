package org.example;

import org.example.ast.Stmt;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

public class Controller {
    public static void main(String[] args) throws Exception {
        String code = """
            const PI = 3.14
            fun area(r: float) -> float {
                return PI * r * r
            }
            fun obama(qwe: int) -> int {
                return qwe * qwe + 5
            }
            print(area(5))
            print(area(6))
            print(obama(8))
        """;
        CodeReaderFroFile str = new CodeReaderFroFile();
        String codeFromFile = str.getCode(new File("/Users/zecqeem/IdeaProjects/Kotlin/src/main/java/org/example/code.txt"));
        RunCode runCode = new RunCode();
        runCode.running(codeFromFile);
        runCode.running(code);
    }
}