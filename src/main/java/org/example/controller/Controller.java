package org.example;
import java.io.File;

public class Controller {
    public static void main(String[] args) throws Exception {
        String code = """
            const PI = 3.14159
            const GREETING = "Hello, world!"
            fun sum(a: int, b: int) -> int {
                return a + b
            }
            fun is_positive(num: float) -> bool {
                if (num > 0) {
                    return true
                } else {
                     return false
                }
            }
            fun calculate_area(radius: float) -> float {
                return PI * radius^2
            }
            var x: int = 10
            var y: float = 5.5
            var z: bool = false
            var name: string = "gorb user"
            print("All tokens and constructions example")
            // Assignment
            x = sum(x, 5)
            print(x) // prints 15
            // Condition
            if (is_positive(y)) {
                print("y is positive")
            } else {
                print("y is not positive")
            }
            // Loop
            while (x > 10) {
                x = x - 1
                print("x is now " + x)
            }
            // Constants and operators
            y = calculate_area(2.0)
            print(y)
            // Boolean expressions
            z = (5 > 3) == true
            print(z)
            // Input
            print("Enter a number: ")
            input(y)
            print("You entered: " + y)
            // String literal and concatenation
            print(GREETING + ", " + name)
        """;
        CodeReaderFroFile str = new CodeReaderFroFile();
        //String codeFromFile = str.getCode(new File("/Users/zecqeem/IdeaProjects/Kotlin/src/main/java/org/example/code.txt"));
        RunCode runCode = new RunCode();
        //runCode.running(codeFromFile);
        runCode.running(code);


    }
}