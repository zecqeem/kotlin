package org.example.ast;
import java.util.List;

public class BlockStmt implements Stmt {
    public final List<Stmt> statements;
    public final int line;
    public BlockStmt(List<Stmt> statements,int line) {
        this.statements = statements;
        this.line = line;
    }

    @Override
    public String toString() {
        return "BlockStmt{" +
                "statements=" + statements +
                '}';
    }
}