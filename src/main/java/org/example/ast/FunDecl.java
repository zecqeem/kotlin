package org.example.ast;
import java.util.List;

public class FunDecl implements Stmt {
    public final String name;
    public final String returnType;
    public final List<VarDecl> params;
    public final BlockStmt body;
    public final int line;// <-- Здесь теперь BlockStmt, а не List<Stmt>

    public FunDecl(String name, String returnType, List<VarDecl> params, BlockStmt body,int line) {
        this.name = name;
        this.returnType = returnType;
        this.params = params;
        this.body = body;
        this.line = line;
    }

    @Override
    public String toString() {
        return "FunDecl{" +
                "name='" + name + '\'' +
                ", returnType='" + returnType + '\'' +
                ", params=" + params +
                ", body=" + body +
                '}';
    }
}