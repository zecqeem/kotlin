package org.example.syntaxAndSemantic;


public class SymbolInfo {
    public final String type;      // "int", "float", "bool", "string"
    public final boolean isConst;  // true якщо це const

    public SymbolInfo(String type, boolean isConst) {
        this.type = type;
        this.isConst = isConst;
    }
}
