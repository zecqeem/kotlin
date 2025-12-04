package org.example.codegen;

import org.example.ast.*;
import org.example.syntaxAndSemantic.SemanticAnalyzer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class CilGenerator {
    private final StringBuilder mainCode = new StringBuilder();
    private final StringBuilder functionsCode = new StringBuilder();

    // Global variables (Class fields)
    private final Map<String, String> globalFields = new LinkedHashMap<>();

    // Local variables and parameters
    private Map<String, String> currentLocals = new LinkedHashMap<>();
    private List<String> currentParams = new ArrayList<>();

    // --- FIX: Store return type of the current function ---
    private String currentReturnType = "void";

    private final Map<String, SemanticAnalyzer.FunSignature> functionTable;
    private final String moduleName;
    private int labelCounter = 1;

    public CilGenerator(String moduleName, Map<String, SemanticAnalyzer.FunSignature> functionTable) {
        this.moduleName = moduleName;
        this.functionTable = functionTable;
    }

    public void generate(List<Stmt> statements) throws IOException {
        // 1. Define global fields
        for (Stmt stmt : statements) {
            if (stmt instanceof VarDecl) {
                VarDecl v = (VarDecl) stmt;
                String type = v.type;
                if (type == null && v.initializer != null) type = inferType(v.initializer);
                if (type == null) type = "int";
                globalFields.put(v.name, mapTypeToCil(type));
            }
        }

        // 2. Generate functions
        for (Stmt stmt : statements) {
            if (stmt instanceof FunDecl) {
                genFunction((FunDecl) stmt);
            }
        }

        // 3. Generate Main
        currentLocals = new LinkedHashMap<>();
        currentParams = new ArrayList<>();
        currentReturnType = "void"; // Main is always void

        collectLocals(statements, currentLocals);

        for (Stmt stmt : statements) {
            if (!(stmt instanceof FunDecl) && !(stmt instanceof VarDecl)) {
                genStmt(stmt, mainCode);
            } else if (stmt instanceof VarDecl) {
                VarDecl v = (VarDecl) stmt;
                if (v.initializer != null) {
                    genExpr(v.initializer, mainCode);
                    String fieldType = globalFields.get(v.name);
                    mainCode.append("    stsfld ").append(fieldType).append(" Program::").append(v.name).append("\n");
                }
            }
        }

        saveToFile();
    }

    private void collectLocals(List<Stmt> statements, Map<String, String> localsMap) {
        for (Stmt stmt : statements) {
            if (stmt instanceof VarDecl) {
                VarDecl v = (VarDecl) stmt;
                if (!globalFields.containsKey(v.name)) {
                    String type = v.type != null ? v.type : "int";
                    if (type == null && v.initializer != null) type = inferType(v.initializer);
                    localsMap.put(v.name, mapTypeToCil(type));
                }
            } else if (stmt instanceof BlockStmt) {
                collectLocals(((BlockStmt) stmt).statements, localsMap);
            } else if (stmt instanceof IfStmt) {
                IfStmt i = (IfStmt) stmt;
                if (i.thenBranch instanceof BlockStmt) collectLocals(((BlockStmt) i.thenBranch).statements, localsMap);
                else collectLocals(Collections.singletonList(i.thenBranch), localsMap);

                if (i.elseBranch != null) {
                    if (i.elseBranch instanceof BlockStmt) collectLocals(((BlockStmt) i.elseBranch).statements, localsMap);
                    else collectLocals(Collections.singletonList(i.elseBranch), localsMap);
                }
            } else if (stmt instanceof WhileStmt) {
                WhileStmt w = (WhileStmt) stmt;
                if (w.body instanceof BlockStmt) collectLocals(((BlockStmt) w.body).statements, localsMap);
                else collectLocals(Collections.singletonList(w.body), localsMap);
            }
        }
    }

    private void saveToFile() throws IOException {
        String filename = moduleName + ".il";
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(".assembly extern mscorlib {}\n");
            writer.write(".assembly " + moduleName + " {}\n");
            writer.write(".module " + moduleName + ".exe\n\n");
            writer.write(".class private auto ansi beforefieldinit Program extends [mscorlib]System.Object {\n");

            for (Map.Entry<String, String> field : globalFields.entrySet()) {
                writer.write("  .field public static " + field.getValue() + " " + field.getKey() + "\n");
            }
            writer.write("\n");
            writer.write(functionsCode.toString());

            writer.write("  .method private hidebysig static void Main(string[] args) cil managed {\n");
            writer.write("    .entrypoint\n");
            if (!currentLocals.isEmpty()) {
                writer.write("    .locals init (\n");
                int i = 0;
                for (Map.Entry<String, String> entry : currentLocals.entrySet()) {
                    writer.write("      [" + i + "] " + entry.getValue() + " " + entry.getKey() + (i == currentLocals.size() - 1 ? "" : ",") + "\n");
                    i++;
                }
                writer.write("    )\n");
            }
            writer.write(mainCode.toString());
            writer.write("    ret\n");
            writer.write("  }\n");
            writer.write("}\n");
        }
        System.out.println("Generated CIL: " + filename);
    }

    private void genFunction(FunDecl f) {
        StringBuilder bodyCode = new StringBuilder();
        Map<String, String> oldLocals = currentLocals;
        List<String> oldParams = currentParams;
        String oldRetType = currentReturnType;

        currentLocals = new LinkedHashMap<>();
        currentParams = new ArrayList<>();
        // --- FIX: Set return type ---
        currentReturnType = f.returnType;

        collectLocals(((BlockStmt)f.body).statements, currentLocals);

        functionsCode.append("\n  .method public hidebysig static ")
                .append(mapTypeToCil(f.returnType))
                .append(" ").append(f.name).append("(");

        for (int i = 0; i < f.params.size(); i++) {
            VarDecl p = f.params.get(i);
            String cilType = mapTypeToCil(p.type);
            functionsCode.append(cilType).append(" ").append(p.name);
            if (i < f.params.size() - 1) functionsCode.append(", ");
            currentParams.add(p.name);
        }
        functionsCode.append(") cil managed {\n");

        for (Stmt s : ((BlockStmt) f.body).statements) {
            genStmt(s, bodyCode);
        }

        if (!currentLocals.isEmpty()) {
            functionsCode.append("    .locals init (\n");
            int i = 0;
            for (Map.Entry<String, String> entry : currentLocals.entrySet()) {
                functionsCode.append("      [").append(i).append("] ").append(entry.getValue()).append(" ").append(entry.getKey())
                        .append(i == currentLocals.size() - 1 ? "" : ",").append("\n");
                i++;
            }
            functionsCode.append("    )\n");
        }

        functionsCode.append(bodyCode);

        if (f.returnType.equals("void") && !bodyCode.toString().contains("ret")) {
            functionsCode.append("    ret\n");
        }
        functionsCode.append("  }\n");

        currentLocals = oldLocals;
        currentParams = oldParams;
        currentReturnType = oldRetType;
    }

    private void genStmt(Stmt stmt, StringBuilder sb) {
        if (stmt instanceof VarDecl) {
            VarDecl v = (VarDecl) stmt;
            if (v.initializer != null) {
                genExpr(v.initializer, sb);
                sb.append("    stloc ").append(v.name).append("\n");
            }
        }
        else if (stmt instanceof AssignStmt) {
            AssignStmt a = (AssignStmt) stmt;
            genExpr(a.value, sb);

            if (currentParams.contains(a.name)) {
                sb.append("    starg ").append(a.name).append("\n");
            } else if (currentLocals.containsKey(a.name)) {
                sb.append("    stloc ").append(a.name).append("\n");
            } else if (globalFields.containsKey(a.name)) {
                String type = globalFields.get(a.name);
                sb.append("    stsfld ").append(type).append(" Program::").append(a.name).append("\n");
            }
        }
        else if (stmt instanceof PrintStmt) {
            Expr expr = ((PrintStmt) stmt).expr;
            genExpr(expr, sb);
            String type = inferType(expr);
            String cilType = mapTypeToCil(type);
            sb.append("    call void [mscorlib]System.Console::WriteLine(").append(cilType).append(")\n");
        }
        else if (stmt instanceof InputStmt) {
            InputStmt inp = (InputStmt) stmt;
            String varName = inp.variableName;

            sb.append("    call string [mscorlib]System.Console::ReadLine()\n");

            String targetType = "int32";
            if (currentLocals.containsKey(varName)) targetType = currentLocals.get(varName);
            else if (globalFields.containsKey(varName)) targetType = globalFields.get(varName);

            if (targetType.equals("int32")) sb.append("    call int32 [mscorlib]System.Convert::ToInt32(string)\n");
            else if (targetType.equals("float32")) sb.append("    call float32 [mscorlib]System.Convert::ToSingle(string)\n");

            if (currentParams.contains(varName)) sb.append("    starg ").append(varName).append("\n");
            else if (currentLocals.containsKey(varName)) sb.append("    stloc ").append(varName).append("\n");
            else if (globalFields.containsKey(varName)) sb.append("    stsfld ").append(targetType).append(" Program::").append(varName).append("\n");
        }
        else if (stmt instanceof IfStmt) {
            IfStmt i = (IfStmt) stmt;
            String elseLabel = newLabel();
            String endLabel = newLabel();
            genExpr(i.condition, sb);
            sb.append("    brfalse ").append(elseLabel).append("\n");
            genStmt(i.thenBranch, sb);
            sb.append("    br ").append(endLabel).append("\n");
            sb.append(elseLabel).append(":\n");
            if (i.elseBranch != null) genStmt(i.elseBranch, sb);
            sb.append(endLabel).append(":\n");
        }
        else if (stmt instanceof WhileStmt) {
            WhileStmt w = (WhileStmt) stmt;
            String startLabel = newLabel();
            String endLabel = newLabel();
            sb.append(startLabel).append(":\n");
            genExpr(w.condition, sb);
            sb.append("    brfalse ").append(endLabel).append("\n");
            genStmt(w.body, sb);
            sb.append("    br ").append(startLabel).append("\n");
            sb.append(endLabel).append(":\n");
        }
        else if (stmt instanceof BlockStmt) {
            for (Stmt s : ((BlockStmt) stmt).statements) genStmt(s, sb);
        }
        else if (stmt instanceof ReturnStmt) {
            genExpr(((ReturnStmt) stmt).expr, sb);

            // --- FIX: Auto-conversion on return ---
            String exprType = inferType(((ReturnStmt) stmt).expr);
            if (currentReturnType.equals("float") && exprType.equals("int")) {
                sb.append("    conv.r4\n"); // int -> float
            }
            // --------------------------------------------

            sb.append("    ret\n");
        }
        else if (stmt instanceof ExprStmt) {
            genExpr(((ExprStmt) stmt).expr, sb);
            sb.append("    pop\n");
        }
    }

    private void genExpr(Expr expr, StringBuilder sb) {
        if (expr instanceof IntLiteral) {
            sb.append("    ldc.i4 ").append(((IntLiteral) expr).value).append("\n");
        }
        else if (expr instanceof FloatLiteral) {
            sb.append("    ldc.r4 ").append(((FloatLiteral) expr).value).append("\n");
        }
        else if (expr instanceof BoolLiteral) {
            sb.append("    ldc.i4.").append(((BoolLiteral) expr).value ? "1" : "0").append("\n");
        }
        else if (expr instanceof StringLiteral) {
            sb.append("    ldstr \"").append(((StringLiteral) expr).value).append("\"\n");
        }
        else if (expr instanceof Variable) {
            String name = ((Variable) expr).name;
            if (currentParams.contains(name)) {
                sb.append("    ldarg ").append(name).append("\n");
            } else if (currentLocals.containsKey(name)) {
                sb.append("    ldloc ").append(name).append("\n");
            } else if (globalFields.containsKey(name)) {
                String type = globalFields.get(name);
                sb.append("    ldsfld ").append(type).append(" Program::").append(name).append("\n");
            } else {
                sb.append("    ldloc ").append(name).append("\n");
            }
        }
        else if (expr instanceof BinaryExpr) {
            BinaryExpr b = (BinaryExpr) expr;
            String typeLeft = inferType(b.left);
            String typeRight = inferType(b.right);

            if (b.op.equals("+") && (typeLeft.equals("string") || typeRight.equals("string"))) {
                genExpr(b.left, sb);
                if (!typeLeft.equals("string")) sb.append("    box [mscorlib]System.").append(getCilBoxType(typeLeft)).append("\n");
                genExpr(b.right, sb);
                if (!typeRight.equals("string")) sb.append("    box [mscorlib]System.").append(getCilBoxType(typeRight)).append("\n");
                sb.append("    call string [mscorlib]System.String::Concat(object, object)\n");
                return;
            }

            if (b.op.equals("^")) {
                genExpr(b.left, sb);
                sb.append("    conv.r8\n");
                genExpr(b.right, sb);
                sb.append("    conv.r8\n");
                sb.append("    call float64 [mscorlib]System.Math::Pow(float64, float64)\n");
                sb.append("    conv.r4\n");
                return;
            }

            genExpr(b.left, sb);
            if (typeLeft.equals("int") && typeRight.equals("float")) sb.append("    conv.r4\n");
            genExpr(b.right, sb);
            if (typeRight.equals("int") && typeLeft.equals("float")) sb.append("    conv.r4\n");

            switch (b.op) {
                case "+": sb.append("    add\n"); break;
                case "-": sb.append("    sub\n"); break;
                case "*": sb.append("    mul\n"); break;
                case "/": sb.append("    div\n"); break;
                case ">": sb.append("    cgt\n"); break;
                case "<": sb.append("    clt\n"); break;
                case "==": sb.append("    ceq\n"); break;
                case "<=":
                    sb.append("    cgt\n");
                    sb.append("    ldc.i4.0\n");
                    sb.append("    ceq\n");
                    break;
                case ">=":
                    sb.append("    clt\n");
                    sb.append("    ldc.i4.0\n");
                    sb.append("    ceq\n");
                    break;
                case "!=":
                    sb.append("    ceq\n");
                    sb.append("    ldc.i4.0\n");
                    sb.append("    ceq\n");
                    break;
                case "&&": sb.append("    and\n"); break;
                case "||": sb.append("    or\n"); break;
                default: throw new RuntimeException("Unknown op: " + b.op);
            }
        }
        else if (expr instanceof CallExpr) {
            CallExpr c = (CallExpr) expr;
            SemanticAnalyzer.FunSignature sig = functionTable.get(c.name);

            for (int i = 0; i < c.args.size(); i++) {
                Expr arg = c.args.get(i);
                genExpr(arg, sb);
                if (sig != null && i < sig.paramTypes.size()) {
                    String expectedType = sig.paramTypes.get(i);
                    String actualType = inferType(arg);
                    if (expectedType.equals("float") && actualType.equals("int")) {
                        sb.append("    conv.r4\n");
                    }
                }
            }

            String retType = (sig != null) ? mapTypeToCil(sig.returnType) : "void";
            StringBuilder argsSig = new StringBuilder();
            if (sig != null) {
                for (int i = 0; i < sig.paramTypes.size(); i++) {
                    if (i > 0) argsSig.append(", ");
                    argsSig.append(mapTypeToCil(sig.paramTypes.get(i)));
                }
            }
            sb.append("    call ").append(retType).append(" Program::").append(c.name).append("(").append(argsSig).append(")\n");
        }
    }

    private String newLabel() { return "L" + (labelCounter++); }

    private String mapTypeToCil(String gorbType) {
        switch (gorbType) {
            case "int": return "int32";
            case "float": return "float32";
            case "bool": return "bool";
            case "string": return "string";
            default: return "void";
        }
    }

    private String getCilBoxType(String gorbType) {
        switch (gorbType) {
            case "int": return "Int32";
            case "float": return "Single";
            case "bool": return "Boolean";
            default: return "Object";
        }
    }

    private String inferType(Expr expr) {
        if (expr instanceof IntLiteral) return "int";
        if (expr instanceof FloatLiteral) return "float";
        if (expr instanceof StringLiteral) return "string";
        if (expr instanceof BoolLiteral) return "bool";
        if (expr instanceof Variable) {
            String name = ((Variable) expr).name;
            if (currentParams.contains(name)) return "int";
            if (currentLocals.containsKey(name)) return inverseMap(currentLocals.get(name));
            if (globalFields.containsKey(name)) return inverseMap(globalFields.get(name));
            return "int";
        }
        if (expr instanceof BinaryExpr) {
            String l = inferType(((BinaryExpr) expr).left);
            String r = inferType(((BinaryExpr) expr).right);
            if (l.equals("string") || r.equals("string")) return "string";
            if (l.equals("float") || r.equals("float")) return "float";
            if (((BinaryExpr) expr).op.matches("(>|<|==|!=|<=|>=)")) return "bool";
            return "int";
        }
        if (expr instanceof CallExpr) {
            SemanticAnalyzer.FunSignature sig = functionTable.get(((CallExpr) expr).name);
            return sig != null ? sig.returnType : "void";
        }
        return "int";
    }

    private String inverseMap(String cilType) {
        if (cilType.equals("int32")) return "int";
        if (cilType.equals("float32")) return "float";
        return cilType;
    }
}