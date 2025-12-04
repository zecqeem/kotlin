package org.example.controller;

import java.io.File;

public class Controller {
    public static void main(String[] args) throws Exception {
        CodeReaderFroFile str = new CodeReaderFroFile();
        String codeFromFile = str.getCode(new File("/Users/zecqeem/IdeaProjects/Kotlin/src/main/java/org/example/code.txt"));
        RunCode runCode = new RunCode();
        runCode.running(codeFromFile);
        //runCode.gorbRun(codeFromFile);
    }

}