package org.example.codegen;

import org.example.ast.*;
import org.example.syntaxAndSemantic.SemanticAnalyzer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class PostfixGenerator {
    private static class Instruction {
        String lexeme;
        String token;

        Instruction(String lexeme, String token) {
            this.lexeme = lexeme;
            this.token = token;
        }

        @Override
        public String toString() {
            return lexeme + " " + token;
        }
    }

    private final List<Instruction> code = new ArrayList<>();

    // Local variables of the current module
    private final Map<String, String> localVars = new LinkedHashMap<>();

    // Global variables (from parent module)
    private final Map<String, String> parentGlobals;

    // Global variables actually used in this function (for .globVarList)
    private final Set<String> usedGlobalVars = new LinkedHashSet<>();

    private final Map<String, Integer> labels = new LinkedHashMap<>();
    private final Set<String> externalFunctions = new LinkedHashSet<>();
    private final Map<String, SemanticAnalyzer.FunSignature> functionTable;

    private int labelCounter = 1;
    private final String moduleName;

    // Constructor for Main module
    public PostfixGenerator(String moduleName, Map<String, SemanticAnalyzer.FunSignature> functionTable) {
        this(moduleName, functionTable, Collections.emptyMap());
    }

    // Constructor for nested functions
    public PostfixGenerator(String moduleName,
                            Map<String, SemanticAnalyzer.FunSignature> functionTable,
                            Map<String, String> parentGlobals) {
        this.moduleName = moduleName;
        this.functionTable = functionTable;
        this.parentGlobals = parentGlobals;
    }

    public void generate(List<Stmt> statements) throws IOException {
        // 1. Collect local variables from ALL blocks (recursive)
        collectLocals(statements);

        // 2. Generate code
        for (Stmt stmt : statements) {
            genStmt(stmt);
        }

        // 3. Save to file
        saveToFile();
    }

    // Recursive variable collection to catch vars inside blocks (e.g. 'check' inside 'while')
    private void collectLocals(List<Stmt> statements) {
        for (Stmt stmt : statements) {
            if (stmt instanceof VarDecl) {
                VarDecl v = (VarDecl) stmt;
                String type = v.type;
                if (type == null && v.initializer != null) type = inferType(v.initializer);
                if (type == null) type = "int";
                localVars.put(v.name, type);
            } else if (stmt instanceof BlockStmt) {
                collectLocals(((BlockStmt) stmt).statements);
            } else if (stmt instanceof IfStmt) {
                IfStmt i = (IfStmt) stmt;
                if (i.thenBranch instanceof BlockStmt) collectLocals(((BlockStmt) i.thenBranch).statements);
                else collectLocals(Collections.singletonList(i.thenBranch));

                if (i.elseBranch != null) {
                    if (i.elseBranch instanceof BlockStmt) collectLocals(((BlockStmt) i.elseBranch).statements);
                    else collectLocals(Collections.singletonList(i.elseBranch));
                }
            } else if (stmt instanceof WhileStmt) {
                WhileStmt w = (WhileStmt) stmt;
                if (w.body instanceof BlockStmt) collectLocals(((BlockStmt) w.body).statements);
                else collectLocals(Collections.singletonList(w.body));
            }
        }
    }

    private String inferType(Expr expr) {
        if (expr instanceof IntLiteral) return "int";
        if (expr instanceof FloatLiteral) return "float";
        if (expr instanceof BoolLiteral) return "bool";
        if (expr instanceof StringLiteral) return "string";

        if (expr instanceof BinaryExpr) {
            BinaryExpr b = (BinaryExpr) expr;
            String l = inferType(b.left);
            String r = inferType(b.right);

            // Boolean logic operators
            if (b.op.matches("(>|<|==|!=|<=|>=)")) return "bool";
            // String concatenation
            if (l.equals("string") || r.equals("string")) return "string";
            // Float propagation
            if (l.equals("float") || r.equals("float")) return "float";

            // --- FIX: Division and Power always return float ---
            if (b.op.equals("^") || b.op.equals("/")) return "float";
            // ------------------------------------------------

            return l;
        }

        if (expr instanceof Variable) {
            String name = ((Variable) expr).name;
            if (localVars.containsKey(name)) return localVars.get(name);
            if (parentGlobals.containsKey(name)) return parentGlobals.get(name);
            return "int";
        }

        if (expr instanceof CallExpr) {
            SemanticAnalyzer.FunSignature sig = functionTable.get(((CallExpr) expr).name);
            return (sig != null) ? sig.returnType : "void";
        }
        return "int";
    }

    private void saveToFile() throws IOException {
        String filename = moduleName + ".postfix";
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(".target: Postfix Machine\n");
            writer.write(".version: 0.2\n\n");

            if (!localVars.isEmpty()) {
                writer.write(".vars(\n");
                for (Map.Entry<String, String> entry : localVars.entrySet()) {
                    writer.write("    " + entry.getKey() + " " + entry.getValue() + "\n");
                }
                writer.write(")\n\n");
            }

            if (!usedGlobalVars.isEmpty()) {
                writer.write(".globVarList(\n");
                for (String varName : usedGlobalVars) {
                    writer.write("    " + varName + "\n");
                }
                writer.write(")\n\n");
            }

            if (!externalFunctions.isEmpty()) {
                writer.write(".funcs(\n");
                for (String funcDecl : externalFunctions) {
                    writer.write("    " + funcDecl + "\n");
                }
                writer.write(")\n\n");
            }

            if (!labels.isEmpty()) {
                writer.write(".labels(\n");
                for (Map.Entry<String, Integer> entry : labels.entrySet()) {
                    writer.write("    " + entry.getKey() + " " + entry.getValue() + "\n");
                }
                writer.write(")\n\n");
            }

            writer.write(".code(\n");
            for (Instruction instr : code) {
                writer.write("    " + instr.lexeme + " " + instr.token + "\n");
            }
            writer.write(")\n");
        }
        System.out.println("Generated PSM: " + filename);
    }

    private void genStmt(Stmt stmt) throws IOException {
        if (stmt instanceof VarDecl) {
            VarDecl v = (VarDecl) stmt;
            if (v.initializer != null) {
                emit(v.name, "l-val");
                genExpr(v.initializer);
                emit(":=", "assign_op");
            }
        }
        else if (stmt instanceof AssignStmt) {
            AssignStmt a = (AssignStmt) stmt;
            if (!localVars.containsKey(a.name) && parentGlobals.containsKey(a.name)) {
                usedGlobalVars.add(a.name);
            }
            emit(a.name, "l-val");
            genExpr(a.value);
            emit(":=", "assign_op");
        }
        else if (stmt instanceof PrintStmt) {
            genExpr(((PrintStmt) stmt).expr);
            emit("OUT", "out_op");
        }
        else if (stmt instanceof InputStmt) {
            InputStmt inp = (InputStmt) stmt;
            String name = inp.variableName;

            if (!localVars.containsKey(name) && parentGlobals.containsKey(name)) {
                usedGlobalVars.add(name);
            }

            String type = localVars.containsKey(name) ? localVars.get(name) : parentGlobals.get(name);
            if (type == null) type = "int";

            emit(name, "l-val");
            emit("INP", "inp_op");
            if (type.equals("int")) emit("s2i", "conv");
            else if (type.equals("float")) emit("s2f", "conv");
            emit(":=", "assign_op");
        }
        else if (stmt instanceof IfStmt) {
            IfStmt i = (IfStmt) stmt;
            String m1 = newLabel();
            String m2 = newLabel();
            genExpr(i.condition);
            emit(m1, "label");
            emit("JF", "jf");
            genStmt(i.thenBranch);
            emit(m2, "label");
            emit("JMP", "jump");
            markLabel(m1);
            if (i.elseBranch != null) genStmt(i.elseBranch);
            markLabel(m2);
        }
        else if (stmt instanceof WhileStmt) {
            WhileStmt w = (WhileStmt) stmt;
            String m1 = newLabel();
            String m2 = newLabel();
            markLabel(m1);
            genExpr(w.condition);
            emit(m2, "label");
            emit("JF", "jf");
            genStmt(w.body);
            emit(m1, "label");
            emit("JMP", "jump");
            markLabel(m2);
        }
        else if (stmt instanceof BlockStmt) {
            for (Stmt s : ((BlockStmt) stmt).statements) genStmt(s);
        }
        else if (stmt instanceof FunDecl) {
            FunDecl f = (FunDecl) stmt;
            // Generate a separate module name for the function
            String funcModuleName = moduleName + "$" + f.name;

            Map<String, String> visibleGlobals = new HashMap<>();
            visibleGlobals.putAll(parentGlobals);
            visibleGlobals.putAll(localVars);

            PostfixGenerator funcGen = new PostfixGenerator(funcModuleName, functionTable, visibleGlobals);

            for (VarDecl param : f.params) {
                funcGen.localVars.put(param.name, param.type);
            }
            funcGen.generate(((BlockStmt) f.body).statements);
        }
        else if (stmt instanceof ReturnStmt) {
            genExpr(((ReturnStmt) stmt).expr);
            emit("RET", "ret_op");
        }
        else if (stmt instanceof ExprStmt) {
            genExpr(((ExprStmt) stmt).expr);
        }
    }

    private void genExpr(Expr expr) {
        if (expr instanceof IntLiteral) {
            emit(String.valueOf(((IntLiteral) expr).value), "int");
        }
        else if (expr instanceof FloatLiteral) {
            emit(String.valueOf(((FloatLiteral) expr).value), "float");
        }
        else if (expr instanceof BoolLiteral) {
            emit(((BoolLiteral) expr).value ? "true" : "false", "bool");
        }
        else if (expr instanceof StringLiteral) {
            emit("\"" + ((StringLiteral) expr).value + "\"", "string");
        }
        else if (expr instanceof Variable) {
            String name = ((Variable) expr).name;
            if (!localVars.containsKey(name) && parentGlobals.containsKey(name)) {
                usedGlobalVars.add(name);
            }
            emit(name, "r-val");
        }
        else if (expr instanceof BinaryExpr) {
            BinaryExpr b = (BinaryExpr) expr;
            String typeLeft = inferType(b.left);
            String typeRight = inferType(b.right);

            // Handle string concatenation
            if (b.op.equals("+") && (typeLeft.equals("string") || typeRight.equals("string"))) {
                genExpr(b.left);
                if (!typeLeft.equals("string")) convertToString(typeLeft);
                genExpr(b.right);
                if (!typeRight.equals("string")) convertToString(typeRight);
                emit("CAT", "cat_op");
                return;
            }

            genExpr(b.left);
            // Implicit coercion int -> float if needed
            if (typeLeft.equals("int") && (typeRight.equals("float") || b.op.equals("^"))) {
                emit("i2f", "conv");
            }

            genExpr(b.right);
            if (typeRight.equals("int") && (typeLeft.equals("float") || b.op.equals("^"))) {
                emit("i2f", "conv");
            }

            switch (b.op) {
                case "+": emit("+", "math_op"); break;
                case "-": emit("-", "math_op"); break;
                case "*": emit("*", "math_op"); break;
                case "/": emit("/", "math_op"); break;
                case "^": emit("^", "math_op"); break;
                case ">": emit(">", "rel_op"); break;
                case "<": emit("<", "rel_op"); break;
                case "==": emit("==", "rel_op"); break;
                case "<=": emit("<=", "rel_op"); break;
                case ">=": emit(">=", "rel_op"); break;
                case "!=": emit("!=", "rel_op"); break;
                case "&&": emit("AND", "bool_op"); break;
                case "||": emit("OR", "bool_op"); break;
                default: throw new RuntimeException("Unknown op: " + b.op);
            }
        }
        else if (expr instanceof CallExpr) {
            CallExpr c = (CallExpr) expr;
            SemanticAnalyzer.FunSignature sig = functionTable.get(c.name);

            for (int i = 0; i < c.args.size(); i++) {
                Expr arg = c.args.get(i);
                genExpr(arg);

                // Check for implicit conversion in arguments
                if (sig != null && i < sig.paramTypes.size()) {
                    String expectedType = sig.paramTypes.get(i);
                    String actualType = inferType(arg);
                    if (expectedType.equals("float") && actualType.equals("int")) {
                        emit("i2f", "conv");
                    }
                }
            }

            String funcName = c.name;
            emit(funcName, "CALL");

            String returnType = (sig != null) ? sig.returnType : "void";
            int argCount = c.args.size();
            externalFunctions.add(funcName + " " + returnType + " " + argCount);
        }
    }

    private void convertToString(String type) {
        if (type.equals("int")) emit("i2s", "conv");
        else if (type.equals("float")) emit("f2s", "conv");
        else if (type.equals("bool")) { emit("b2i", "conv"); emit("i2s", "conv"); }
    }

    private void emit(String lexeme, String token) {
        code.add(new Instruction(lexeme, token));
    }

    private String newLabel() {
        return "m" + (labelCounter++);
    }

    private void markLabel(String label) {
        labels.put(label, code.size());
        emit(label, "label");
        emit(":", "colon");
    }
}