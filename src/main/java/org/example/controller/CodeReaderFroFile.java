package org.example;

import java.io.*;

public class CodeReaderFroFile {
    public String getCode(File file){
        return createString(file);
    }
    private String createString(File file){
        try(BufferedReader fileToRead = new BufferedReader(new FileReader(file))){
            String line;
            StringBuilder str = new StringBuilder();
            while ((line = fileToRead.readLine()) != null) {
                str.append(line);
            }
            return str.toString();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
