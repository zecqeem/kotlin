package org.example.ast;



public class FunDecl implements Stmt {
    public final String name;
    public final String returnType;
    public final java.util.List<VarDecl> params;
    public final java.util.List<Stmt> body;
    public FunDecl(String name, String returnType, java.util.List<VarDecl> params, java.util.List<Stmt> body) {
        this.name = name;
        this.returnType = returnType;
        this.params = params;
        this.body = body;
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
