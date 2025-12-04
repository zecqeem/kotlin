package org.example.controller;

import java.io.*;

public class CodeReaderFroFile {
    public String getCode(File file){
        return createString(file);
    }
    private String createString(File file){
        try(BufferedReader fileToRead = new BufferedReader(new FileReader(file))){
            String line;
            StringBuilder str = new StringBuilder();

            // Читаємо перший рядок
            if ((line = fileToRead.readLine()) != null) {
                str.append(line);
            }

            // Читаємо всі наступні рядки, додаючи "\n" перед кожним новим рядком
            while ((line = fileToRead.readLine()) != null) {
                str.append("\n"); // !!! ДОДАЄМО СИМВОЛ НОВОГО РЯДКА !!!
                str.append(line);
            }
            return str.toString();
        } catch (FileNotFoundException e) {
            // ... (обробка помилок)
            throw new RuntimeException("File not found: " + file.getName(), e);
        } catch (IOException e) {
            // ... (обробка помилок)
            throw new RuntimeException("Error reading file: " + file.getName(), e);
        }
    }
}