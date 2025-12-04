package org.example.syntaxAndSemantic;

import org.example.ast.*;
import java.util.*;

public class SemanticAnalyzer {
    // Stack for scopes (local variable environments)
    private final Stack<Map<String, SymbolInfo>> scopes = new Stack<>();
    // Global function registry
    private final Map<String, FunSignature> functions = new HashMap<>();

    // Context for current function analysis
    private String currentFunctionReturnType = null;
    private String currentFunctionName = "global scope";

    /**
     * Stores function signature details.
     */
    public static class FunSignature {
        public String returnType;
        public List<String> paramTypes;

        public FunSignature(String returnType, List<String> paramTypes) {
            this.returnType = returnType;
            this.paramTypes = paramTypes;
        }
    }

    public SemanticAnalyzer() {
        // Initialize global scope
        scopes.push(new HashMap<>());
    }

    public void analyze(List<Stmt> statements) {
        for (Stmt stmt : statements) {
            analyzeStmt(stmt);
        }
    }

    private void analyzeStmt(Stmt stmt) {
        if (stmt instanceof VarDecl) {
            analyzeVarDecl((VarDecl) stmt);
        } else if (stmt instanceof AssignStmt) {
            analyzeAssign((AssignStmt) stmt);
        } else if (stmt instanceof PrintStmt) {
            analyzeExpr(((PrintStmt) stmt).expr);
        } else if (stmt instanceof IfStmt) {
            analyzeIf((IfStmt) stmt);
        } else if (stmt instanceof WhileStmt) {
            analyzeWhile((WhileStmt) stmt);
        } else if (stmt instanceof BlockStmt) {
            analyzeBlock((BlockStmt) stmt);
        } else if (stmt instanceof FunDecl) {
            analyzeFunDecl((FunDecl) stmt);
        } else if (stmt instanceof ExprStmt) {
            analyzeExpr(((ExprStmt) stmt).expr);
        } else if (stmt instanceof InputStmt) {
            InputStmt input = (InputStmt) stmt;
            // Ensure variable exists and is not a constant
            SymbolInfo info = resolveVariable(input.variableName, input.line);
            checkNotConst(input.variableName, input.line);
        } else if (stmt instanceof ReturnStmt) {
            analyzeReturn((ReturnStmt) stmt);
        }
    }

    private void analyzeReturn(ReturnStmt stmt) {
        if (currentFunctionReturnType == null) {
            throw new RuntimeException("Error at line " + stmt.line + ": Return statement outside of function");
        }

        String exprType = analyzeExpr(stmt.expr);

        // Check return type compatibility
        if (!exprType.equals(currentFunctionReturnType)) {
            // Allow implicit conversion: int -> float
            if (!(currentFunctionReturnType.equals("float") && exprType.equals("int"))) {
                throw new RuntimeException("Error at line " + stmt.line + " in function '" + currentFunctionName +
                        "': Type mismatch in return. Expected " + currentFunctionReturnType + ", got " + exprType);
            }
        }
    }

    private void analyzeFunDecl(FunDecl stmt) {
        List<String> paramTypes = new ArrayList<>();
        for (VarDecl param : stmt.params) {
            paramTypes.add(param.type);
        }

        // Check for function redeclaration
        if (functions.containsKey(stmt.name)) {
            throw new RuntimeException("Error at line " + stmt.line + ": Function '" + stmt.name + "' already declared.");
        }

        functions.put(stmt.name, new FunSignature(stmt.returnType, paramTypes));

        enterScope();

        // Save previous context
        String previousReturnType = currentFunctionReturnType;
        String previousFunctionName = currentFunctionName;

        // Set new context
        currentFunctionReturnType = stmt.returnType;
        currentFunctionName = stmt.name;

        try {
            // Declare parameters in the local scope
            for (VarDecl param : stmt.params) {
                declareVariable(param.name, param.type, false, param.line);
            }
            // Analyze function body
            for (Stmt s : stmt.body.statements) {
                analyzeStmt(s);
            }
        } finally {
            // Restore previous context
            currentFunctionReturnType = previousReturnType;
            currentFunctionName = previousFunctionName;
            exitScope();
        }
    }

    private void analyzeVarDecl(VarDecl stmt) {
        String declaredType = stmt.type;
        String inferredType = null;

        if (stmt.initializer != null) {
            inferredType = analyzeExpr(stmt.initializer);
        }

        if (stmt.isConstant) {
            if (inferredType == null)
                throw new RuntimeException("Error at line " + stmt.line + ": Constant '" + stmt.name + "' must have an initializer.");
            declaredType = inferredType; // Constants infer type from initializer
        } else {
            if (declaredType != null && inferredType != null) {
                if (!declaredType.equals(inferredType)) {
                    // Allow implicit int -> float conversion
                    if (!(declaredType.equals("float") && inferredType.equals("int"))) {
                        throw new RuntimeException("Error at line " + stmt.line + ": Type mismatch for variable '" + stmt.name +
                                "': declared " + declaredType + ", got " + inferredType);
                    }
                }
            }
        }
        declareVariable(stmt.name, declaredType, stmt.isConstant, stmt.line);
    }

    private void analyzeAssign(AssignStmt stmt) {
        SymbolInfo info = resolveVariable(stmt.name, stmt.line);
        if (info.isConst)
            throw new RuntimeException("Error at line " + stmt.line + ": Cannot reassign constant '" + stmt.name + "'");

        String exprType = analyzeExpr(stmt.value);
        if (!info.type.equals(exprType)) {
            // Allow implicit int -> float conversion
            if (!(info.type.equals("float") && exprType.equals("int"))) {
                throw new RuntimeException("Error at line " + stmt.line + ": Type mismatch on assignment to '" + stmt.name +
                        "': expected " + info.type + ", got " + exprType);
            }
        }
    }

    private void analyzeIf(IfStmt stmt) {
        if (!analyzeExpr(stmt.condition).equals("bool"))
            throw new RuntimeException("Error at line " + stmt.line + ": 'if' condition must be of type bool");
        analyzeStmt(stmt.thenBranch);
        if (stmt.elseBranch != null) analyzeStmt(stmt.elseBranch);
    }

    private void analyzeWhile(WhileStmt stmt) {
        if (!analyzeExpr(stmt.condition).equals("bool"))
            throw new RuntimeException("Error at line " + stmt.line + ": 'while' condition must be of type bool");
        analyzeStmt(stmt.body);
    }

    private void analyzeBlock(BlockStmt block) {
        enterScope();
        try {
            for (Stmt s : block.statements) analyzeStmt(s);
        } finally {
            exitScope();
        }
    }

    // === Expression Analysis ===

    private String analyzeExpr(Expr expr) {
        if (expr instanceof IntLiteral) return "int";
        if (expr instanceof FloatLiteral) return "float";
        if (expr instanceof BoolLiteral) return "bool";
        if (expr instanceof StringLiteral) return "string";

        if (expr instanceof Variable) {
            Variable v = (Variable) expr;
            return resolveVariable(v.name, v.line).type;
        }

        if (expr instanceof BinaryExpr) return analyzeBinaryExpr((BinaryExpr) expr);

        if (expr instanceof CallExpr) return analyzeCall((CallExpr) expr);

        throw new RuntimeException("Error: Unknown expression type: " + expr);
    }

    private String analyzeBinaryExpr(BinaryExpr expr) {
        String left = analyzeExpr(expr.left);
        String right = analyzeExpr(expr.right);

        // Division check
        if (expr.op.equals("/")) {
            // Basic static check for division by zero literals
            if (expr.right instanceof IntLiteral && ((IntLiteral) expr.right).value == 0) {
                throw new RuntimeException("Error at line " + expr.line + ": Division by zero detected (int)");
            }
            if (expr.right instanceof FloatLiteral && ((FloatLiteral) expr.right).value == 0.0) {
                throw new RuntimeException("Error at line " + expr.line + ": Division by zero detected (float)");
            }
            if ((left.equals("int") || left.equals("float")) && (right.equals("int") || right.equals("float"))) {
                return "float"; // Division always results in float
            }
        }

        if (Set.of("+", "-", "*", "^").contains(expr.op)) {
            // String concatenation
            if (expr.op.equals("+") && (left.equals("string") || right.equals("string"))) return "string";

            boolean isFloat = left.equals("float") || right.equals("float");
            if ((left.equals("int") || left.equals("float")) && (right.equals("int") || right.equals("float"))) {
                return isFloat ? "float" : "int";
            }
            throw new RuntimeException("Error at line " + expr.line + ": Operator '" + expr.op + "' not valid for types " + left + " and " + right);
        }

        if (Set.of(">", "<", ">=", "<=").contains(expr.op)) {
            if ((left.equals("int") || left.equals("float")) && (right.equals("int") || right.equals("float"))) {
                return "bool";
            }
            throw new RuntimeException("Error at line " + expr.line + ": Comparison operator '" + expr.op + "' requires numeric types");
        }

        if (Set.of("==", "!=", "EQ", "NEQ").contains(expr.op)) {
            return "bool";
        }
        return "unknown";
    }

    private String analyzeCall(CallExpr expr) {
        FunSignature func = functions.get(expr.name);
        if (func == null)
            throw new RuntimeException("Error at line " + expr.line + ": Undefined function '" + expr.name + "'");

        if (func.paramTypes.size() != expr.args.size())
            throw new RuntimeException("Error at line " + expr.line + ": Argument count mismatch for function '" + expr.name + "'");

        for(int i=0; i<expr.args.size(); i++) {
            String argType = analyzeExpr(expr.args.get(i));
            String expected = func.paramTypes.get(i);
            if (!argType.equals(expected)) {
                // Allow int -> float
                if (!(expected.equals("float") && argType.equals("int"))) {
                    throw new RuntimeException("Error at line " + expr.line + ": Argument " + (i+1) +
                            " mismatch in function call '" + expr.name + "': expected " + expected + ", got " + argType);
                }
            }
        }
        return func.returnType;
    }

    // === Scopes ===
    private void enterScope() { scopes.push(new HashMap<>()); }
    private void exitScope() { scopes.pop(); }

    private void declareVariable(String name, String type, boolean isConst, int line) {
        // 1. Check for duplicate variable declaration in the current scope
        if (scopes.peek().containsKey(name)) {
            throw new RuntimeException("Error at line " + line + ": Variable '" + name + "' already declared in this scope.");
        }

        // 2. Check for conflict with function names
        if (functions.containsKey(name)) {
            throw new RuntimeException("Error at line " + line + ": Identifier '" + name + "' is already declared as a function.");
        }

        // Declare variable
        scopes.peek().put(name, new SymbolInfo(type, isConst));
    }

    private SymbolInfo resolveVariable(String name, int line) {
        // Search from inner scope to outer scope
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name)) return scopes.get(i).get(name);
        }
        throw new RuntimeException("Error at line " + line + ": Undefined variable '" + name + "'");
    }

    private void checkNotConst(String name, int line) {
        if(resolveVariable(name, line).isConst)
            throw new RuntimeException("Error at line " + line + ": Cannot modify constant '" + name + "'");
    }

    public Map<String, FunSignature> getFunctions() {
        return functions;
    }
}