package org.example;
import java.io.File;

public class Controller {
    public static void main(String[] пам) throws Exception {
        String code = """
            const PI = 3.14
            const CHECK = 2
            //пизда
            bool checkBool = true
            int existInt = 2
            float checkReal = 3.1
            fun area(r: float) -> float {
                return PI * r * r
            }
            fun obama(first: int, second: int) -> int {
                return first * first + 5 + second + CHECK
            }
            print(area(5))
            print(obama(1,existInt))
            print(checkBool)
            print(checkReal)
            //print(2 == 2)
        """;
        CodeReaderFroFile str = new CodeReaderFroFile();
        //String codeFromFile = str.getCode(new File("/Users/zecqeem/IdeaProjects/Kotlin/src/main/java/org/example/code.txt"));
        RunCode runCode = new RunCode();
        //runCode.running(codeFromFile);
        runCode.running(code);


    }
}