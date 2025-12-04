package org.example.controller;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.example.ast.*;
import org.example.codegen.CilGenerator;
import org.example.codegen.PostfixGenerator;
import org.example.GorbLexer;
import org.example.GorbParser;

import org.example.lexer.Lexer;
import org.example.syntaxAndSemantic.Parser;
import org.example.syntaxAndSemantic.SemanticAnalyzer;
import org.example.tokens.Token;

import java.util.List;

public class RunCode {

    // Legacy pipeline (Labs 1-5: Manual Lexer/Parser + Codegen)
    public void running(String code) throws Exception {
        try {
            System.out.println("SYNTAX ANALYSIS");
            Lexer lexer = new Lexer(code);
            List<Token> tokens = lexer.tokenize();

            Parser parser = new Parser(tokens);
            List<Stmt> statements = parser.parse();

            // Pretty print the AST to visualize nesting
            // Note: Ensure AstPrinter class exists in your project
            AstPrinter printer = new AstPrinter();
            printer.print(statements);

            System.out.println("\nSEMANTIC ANALYSIS");
            SemanticAnalyzer semantic = new SemanticAnalyzer();
            semantic.analyze(statements);

            // If execution reaches here, no errors occurred
            System.out.println("SUCCESS");

            System.out.println("\n=== CODE GENERATION ===");
            PostfixGenerator generator = new PostfixGenerator("main", semantic.getFunctions());
            generator.generate(statements);
            System.out.println("Postfix code generation finished.");

            CilGenerator cilGenerator = new CilGenerator("main", semantic.getFunctions());
            cilGenerator.generate(statements);
            System.out.println("CIL code generation finished.");

        } catch (Exception e) {
            System.err.println("ERROR " + e.getMessage());
        }
    }

    // ANTLR pipeline (Lab 6: ANTLR Lexer/Parser + Evaluator)
    public void gorbRun(String code) {
        CharStream input = CharStreams.fromString(code);
        GorbLexer lexer = new GorbLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        GorbParser parser = new GorbParser(tokens);

        ParseTree tree = parser.program();

        if (parser.getNumberOfSyntaxErrors() > 0) {
            System.out.println("❌ Syntax error");
            return;
        }

        System.out.println("--- STARTING EXECUTION (GorbEvaluator) ---");

        GorbEvaluator evaluator = new GorbEvaluator();
        evaluator.visit(tree);

        System.out.println("\n--- EXECUTION FINISHED ---");
    }
}