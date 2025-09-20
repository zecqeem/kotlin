package org.example;

import org.example.ast.*;

import javax.tools.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class CodeGenerator {
    private final List<Stmt> program;
    private final String className = "CompiledProgram";

    public CodeGenerator(List<Stmt> program) {
        this.program = program;
    }

    public Class<?> compileAndLoad() throws Exception {
        String source = generateSource();
        Path tmpDir = Files.createTempDirectory("toycompiler");
        // write source
        Path srcFile = tmpDir.resolve(className + ".java");
        Files.write(srcFile, source.getBytes(StandardCharsets.UTF_8));

        // compile
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("No system JavaCompiler found (run with JDK).");
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        Iterable<? extends JavaFileObject> compilationUnits =
                fileManager.getJavaFileObjectsFromFiles(Collections.singletonList(srcFile.toFile()));
        List<String> options = Arrays.asList("-d", tmpDir.toString());
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, options, null, compilationUnits);
        boolean ok = task.call();
        fileManager.close();
        if (!ok) throw new RuntimeException("Compilation failed. Source:\n" + source);

        URLClassLoader loader = new URLClassLoader(new URL[]{tmpDir.toUri().toURL()});
        return loader.loadClass(className);
    }

    private String generateSource() {
        StringBuilder sb = new StringBuilder();
        sb.append("import java.util.*;\n");
        sb.append("public class ").append(className).append(" {\n");

        // 1) поля для const
        for (Stmt s : program) {
            if (s instanceof VarDecl) {
                VarDecl vd = (VarDecl) s;
                if (vd.value != null) {
                    String jType = mapTypeName(vd.type); // vd.type может быть null (если не указан)
                    if (jType == null) {
                        // infer type from literal if possible
                        jType = inferTypeName(vd.value);
                        if (jType == null) jType = "Object";
                    }
                    sb.append("  public static final ").append(jType).append(" ").append(vd.name)
                            .append(" = ").append(exprToJava(vd.value)).append(";\n");
                }
            }
        }

        // 2) функции
        for (Stmt s : program) {
            if (s instanceof FunDecl) {
                FunDecl fd = (FunDecl) s;
                String retType = mapTypeName(fd.returnType);
                if (retType == null) retType = "void";
                sb.append("  public static ").append(retType).append(" ").append(fd.name).append("(");
                // params
                List<String> params = new ArrayList<>();
                for (VarDecl p : fd.params) {
                    String pType = mapTypeName(p.type);
                    if (pType == null) pType = "Object";
                    params.add(pType + " " + p.name);
                }
                sb.append(String.join(", ", params));
                sb.append(") {\n");
                // body
                for (Stmt bs : fd.body) {
                    sb.append("    ").append(stmtToJava(bs)).append("\n");
                }
                sb.append("  }\n");
            }
        }

        // 3) main: выполнит остальные топ-уровнев statements (например print(...))
        sb.append("  public static void main(String[] args) {\n");
        for (Stmt s : program) {
            if (!(s instanceof VarDecl) && !(s instanceof FunDecl)) {
                sb.append("    ").append(stmtToJava(s)).append("\n");
            }
        }
        sb.append("  }\n");

        sb.append("}\n");
        return sb.toString();
    }

    // Преобразует statement в Java-код (однострочно, с точкой с запятой)
    private String stmtToJava(Stmt s) {
        if (s instanceof PrintStmt) {
            PrintStmt p = (PrintStmt) s;
            return "System.out.println(" + exprToJava(p.expr) + ");";
        } else if (s instanceof ExprStmt) {
            return exprToJava(((ExprStmt) s).expr) + ";";
        } else if (s instanceof VarDecl) {
            VarDecl v = (VarDecl) s;
            // если константа с value — уже объявлена как static final; но если попала сюда без value, объявим локальную переменную
            if (v.value != null) {
                // top-level handled earlier; local fallback:
                String jType = mapTypeName(v.type);
                if (jType == null) jType = inferTypeName(v.value);
                if (jType == null) jType = "Object";
                return jType + " " + v.name + " = " + exprToJava(v.value) + ";";
            } else {
                String jType = mapTypeName(v.type);
                if (jType == null) jType = "Object";
                return jType + " " + v.name + ";";
            }
        } else if (s instanceof ReturnStmt) {
            ReturnStmt r = (ReturnStmt) s;
            return "return " + exprToJava(r.expr) + ";";
        } else if (s instanceof FunDecl) {
            // handled separately
            return "// function " + ((FunDecl) s).name;
        } else {
            return "// unsupported stmt: " + s;
        }
    }

    // Генерация выражений в Java-код (строка)
    private String exprToJava(Expr e) {
        if (e instanceof IntLiteral) {
            return String.valueOf(((IntLiteral) e).value);
        } else if (e instanceof FloatLiteral) {
            // double literal: ensure decimal point
            double v = ((FloatLiteral) e).value;
            if (Double.isFinite(v) && (v == (long) v)) {
                return String.format("%d.0", (long) v);
            }
            return Double.toString(v);
        } else if (e instanceof Variable) {
            return ((Variable) e).name;
        } else if (e instanceof BinaryExpr) {
            BinaryExpr b = (BinaryExpr) e;
            // surround with parentheses to preserve precedence
            return "(" + exprToJava(b.left) + " " + b.op + " " + exprToJava(b.right) + ")";
        } else if (e instanceof CallExpr) {
            CallExpr c = (CallExpr) e;
            List<String> args = new ArrayList<>();
            for (Expr a : c.args) args.add(exprToJava(a));
            return c.name + "(" + String.join(", ", args) + ")";
        } else {
            return "/* unsupported expr */ null";
        }
    }

    // Карта типа языка -> Java
    private String mapTypeName(String t) {
        if (t == null) return null;
        switch (t) {
            case "int": return "int";
            case "float": return "double";
            case "bool": return "boolean";
            case "string": return "String";
            default: return null;
        }
    }

    // Попытка вывести Java-тип по литералу
    private String inferTypeName(Expr e) {
        if (e instanceof IntLiteral) return "int";
        if (e instanceof FloatLiteral) return "double";
        if (e instanceof CallExpr) return null;
        if (e instanceof Variable) return null;
        return null;
    }
}