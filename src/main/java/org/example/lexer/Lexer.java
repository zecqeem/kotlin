package org.example.lexer;

import org.example.tokens.Token;
import org.example.tokens.TokenType;

import java.util.*;

public class Lexer {
    private final String input;
    private int pos = 0;
    private int line = 1;

    // Output Tables
    public final List<String> tableOfSymb = new ArrayList<>();
    private final Map<String, Integer> tableOfId = new LinkedHashMap<>();
    private final Map<String, String> tableOfConst = new LinkedHashMap<>();

    //  DFA States
    private static final int S_START = 0;
    // ID / Keywords
    private static final int S_ID = 1;
    private static final int S_ID_FIN = 2;  // *
    // Numbers
    private static final int S_INT = 3;
    private static final int S_INT_FIN = 4; // *
    private static final int S_DOT = 5;
    private static final int S_FLOAT = 6;
    private static final int S_FLOAT_FIN = 7; // *
    // Operators
    private static final int S_MINUS = 8;
    private static final int S_ARROW = 9;   // ->
    private static final int S_MINUS_FIN = 10; // * (-)
    private static final int S_EQ = 11;
    private static final int S_EQ_EQ = 12;  // ==
    private static final int S_EQ_FIN = 13; // * (=)
    // Strings
    private static final int S_STR_START = 14;
    private static final int S_STR_BODY = 15;
    private static final int S_STR_FIN = 16;
    // Comments & Division
    private static final int S_DIV = 17;
    private static final int S_COMMENT = 18;
    private static final int S_DIV_FIN = 19; // * (/)
    // Comparison > >= < <= ! !=
    private static final int S_GT = 20;
    private static final int S_GT_EQ = 21;  // >=
    private static final int S_GT_FIN = 22; // * (>)
    private static final int S_LT = 23;
    private static final int S_LT_EQ = 24;  // <=
    private static final int S_LT_FIN = 25; // * (<)
    private static final int S_NOT = 26;
    private static final int S_NOT_EQ = 27; // !=
    private static final int S_NOT_FIN = 28; // * (!)
    // Single Char Ops (+ * ^ ( ) { } , : ;)
    private static final int S_OP = 99;

    // ERROR STATE
    private static final int S_ERR = 100;

    // States that require putCharBack (Star states)
    private static final Set<Integer> starStates = Set.of(
            S_ID_FIN, S_INT_FIN, S_FLOAT_FIN,
            S_MINUS_FIN, S_EQ_FIN, S_DIV_FIN,
            S_GT_FIN, S_LT_FIN, S_NOT_FIN
    );

    private enum CharClass {
        LETTER, DIGIT, DOT, WS, NL, QUOTE,
        MINUS, GT, LT, EQ, SLASH, EXCL, OP, OTHER
    }

    private final Map<Integer, Map<CharClass, Integer>> stf = new HashMap<>();

    public Lexer(String input) {
        this.input = input;
        initTransitions();
    }

    private void addTrans(int state, CharClass cls, int next) {
        stf.computeIfAbsent(state, k -> new HashMap<>()).put(cls, next);
    }

    private void initTransitions() {
        // 0: Start
        addTrans(S_START, CharClass.LETTER, S_ID);
        addTrans(S_START, CharClass.DIGIT, S_INT);
        addTrans(S_START, CharClass.QUOTE, S_STR_START);
        addTrans(S_START, CharClass.MINUS, S_MINUS);
        addTrans(S_START, CharClass.EQ, S_EQ);
        addTrans(S_START, CharClass.SLASH, S_DIV);
        addTrans(S_START, CharClass.GT, S_GT);
        addTrans(S_START, CharClass.LT, S_LT);
        addTrans(S_START, CharClass.EXCL, S_NOT);
        addTrans(S_START, CharClass.OP, S_OP);
        // Error handling for Start (Unexpected chars like @, #, etc.)
        addTrans(S_START, CharClass.OTHER, S_ERR);

        // 1: ID
        addTrans(S_ID, CharClass.LETTER, S_ID);
        addTrans(S_ID, CharClass.DIGIT, S_ID);
        addTrans(S_ID, CharClass.OTHER, S_ID_FIN); // *

        // Numbers
        addTrans(S_INT, CharClass.DIGIT, S_INT);
        addTrans(S_INT, CharClass.DOT, S_DOT);
        addTrans(S_INT, CharClass.OTHER, S_INT_FIN); // *

        // Dot logic: Digit -> Float, Other -> ERROR (e.g. "12.a")
        addTrans(S_DOT, CharClass.DIGIT, S_FLOAT);
        addTrans(S_DOT, CharClass.OTHER, S_ERR);

        addTrans(S_FLOAT, CharClass.DIGIT, S_FLOAT);
        addTrans(S_FLOAT, CharClass.OTHER, S_FLOAT_FIN); // *

        // Operators
        addTrans(S_MINUS, CharClass.GT, S_ARROW);     // ->
        addTrans(S_MINUS, CharClass.OTHER, S_MINUS_FIN); // * -
        addTrans(S_EQ, CharClass.EQ, S_EQ_EQ);        // ==
        addTrans(S_EQ, CharClass.OTHER, S_EQ_FIN);    // * =
        addTrans(S_DIV, CharClass.SLASH, S_COMMENT);  // //
        addTrans(S_DIV, CharClass.OTHER, S_DIV_FIN);  // * /

        // Comparisons
        addTrans(S_GT, CharClass.EQ, S_GT_EQ);        // >=
        addTrans(S_GT, CharClass.OTHER, S_GT_FIN);    // * >
        addTrans(S_LT, CharClass.EQ, S_LT_EQ);        // <=
        addTrans(S_LT, CharClass.OTHER, S_LT_FIN);    // * <
        addTrans(S_NOT, CharClass.EQ, S_NOT_EQ);      // !=
        addTrans(S_NOT, CharClass.OTHER, S_NOT_FIN);  // * !

        // Strings (Correct Logic)
        // 1. Empty string case: "" -> Fin
        addTrans(S_STR_START, CharClass.QUOTE, S_STR_FIN);

        // 2. Body logic
        for (CharClass c : CharClass.values()) {
            if (c != CharClass.QUOTE) {
                // From Start to Body (non-empty string start)
                addTrans(S_STR_START, c, S_STR_BODY);
                // Within Body (continue string)
                addTrans(S_STR_BODY, c, S_STR_BODY);
            }
        }
        // 3. Closing quote
        addTrans(S_STR_BODY, CharClass.QUOTE, S_STR_FIN);
    }

    private CharClass classOfChar(char c) {
        if (Character.isLetter(c) || c == '_') return CharClass.LETTER;
        if (Character.isDigit(c)) return CharClass.DIGIT;
        if (c == '.') return CharClass.DOT;
        if (c == ' ' || c == '\t') return CharClass.WS;
        if (c == '\n') return CharClass.NL;
        if (c == '"') return CharClass.QUOTE;
        if (c == '-') return CharClass.MINUS;
        if (c == '>') return CharClass.GT;
        if (c == '<') return CharClass.LT;
        if (c == '=') return CharClass.EQ;
        if (c == '/') return CharClass.SLASH;
        if (c == '!') return CharClass.EXCL;
        // Added semicolon ';' to operators
        if ("+*^(){},:;".indexOf(c) != -1) return CharClass.OP;
        return CharClass.OTHER;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        int state = S_START;
        StringBuilder lexeme = new StringBuilder();

        while (pos < input.length()) {
            char c = input.charAt(pos);

            // 1. Comment Handling
            if (state == S_COMMENT) {
                if (c == '\n') {
                    state = S_START;
                    line++;
                    lexeme.setLength(0); // Clear buffer
                }
                pos++;
                continue;
            }

            CharClass cls = classOfChar(c);
            Integer nextState = null;

            if (stf.containsKey(state)) nextState = stf.get(state).get(cls);

            // =FIX HERE
            // Check if it is a whitespace/newline in the start state.
            // If yes, disable fallback to OTHER (to avoid going to S_ERR).
            boolean isSpaceInStart = (state == S_START && (cls == CharClass.WS || cls == CharClass.NL));

            if (!isSpaceInStart && nextState == null && stf.containsKey(state) && stf.get(state).containsKey(CharClass.OTHER)) {
                nextState = stf.get(state).get(CharClass.OTHER);
            }

            // 2. Error / Skip handling
            if (nextState == null) {
                // If we are in Start and see Whitespace or NewLine, skip and update line count
                if (state == S_START && (cls == CharClass.WS || cls == CharClass.NL)) {
                    if (cls == CharClass.NL) line++;
                    pos++;
                    continue;
                }
                // Fallback panic
                throw new RuntimeException("Lexer Error line " + line + ": Unexpected char '" + c + "'");
            }

            // EXPLICIT ERROR STATE CHECK
            if (nextState == S_ERR) {
                throw new RuntimeException("Lexer Error line " + line + ": Invalid syntax or unexpected char '" + c + "'");
            }

            // 3. Transition Logic
            if (starStates.contains(nextState)) {
                // Star state: Finalize, Put Char Back (do not inc pos)
                processing(nextState, lexeme.toString(), tokens);
                lexeme.setLength(0);
                state = S_START;
            } else {
                // Direct Transitions (consume char)

                // Special case: Single Char Operators or Double Operators
                if (nextState == S_EQ_EQ || nextState == S_ARROW ||
                        nextState == S_GT_EQ || nextState == S_LT_EQ ||
                        nextState == S_NOT_EQ || nextState == S_OP) {

                    lexeme.append(c);
                    processing(nextState, lexeme.toString(), tokens);
                    lexeme.setLength(0);
                    state = S_START;
                }
                // String Final (consume closing quote)
                else if (nextState == S_STR_FIN) {
                    lexeme.append(c);
                    processing(nextState, lexeme.toString(), tokens);
                    lexeme.setLength(0);
                    state = S_START;
                }
                // Comment Start (clear buffer of slashes)
                else if (nextState == S_COMMENT) {
                    lexeme.setLength(0);
                    state = S_COMMENT;
                }
                // Normal Accumulation
                else {
                    if (nextState != S_START) {
                        lexeme.append(c);
                    }
                    state = nextState;
                }
                pos++;
            }
        }

        // EOF Handling
        if (lexeme.length() > 0) {
            if (state == S_ID || state == S_INT || state == S_FLOAT) {
                int finState = (state == S_ID) ? S_ID_FIN : (state == S_INT) ? S_INT_FIN : S_FLOAT_FIN;
                processing(finState, lexeme.toString(), tokens);
            }
        }
        tokens.add(new Token(TokenType.EOF, "",line));

        printTables();
        return tokens;
    }

    private void processing(int state, String lexeme, List<Token> tokens) {
        if (lexeme.isEmpty()) return;

        TokenType type;
        String idx = "";

        switch (state) {
            case S_ID_FIN:
                type = getKeyword(lexeme);
                if (type == TokenType.IDENTIFIER) {
                    if (!tableOfId.containsKey(lexeme)) {
                        tableOfId.put(lexeme, tableOfId.size() + 1);
                    }
                    idx = String.valueOf(tableOfId.get(lexeme));
                }
                break;
            case S_INT_FIN:
                type = TokenType.INT;
                addToConstTable(lexeme, "int");
                idx = String.valueOf(tableOfConst.size());
                break;
            case S_FLOAT_FIN:
                type = TokenType.FLOAT;
                addToConstTable(lexeme, "float");
                idx = String.valueOf(tableOfConst.size());
                break;
            case S_STR_FIN:
                type = TokenType.STRING;
                addToConstTable(lexeme, "string");
                idx = String.valueOf(tableOfConst.size());
                break;
            // Operators
            case S_EQ_FIN: type = TokenType.ASSIGN; break;
            case S_EQ_EQ: type = TokenType.EQ; break;
            case S_ARROW: type = TokenType.ARROW; break;
            case S_MINUS_FIN: type = TokenType.MINUS; break;
            case S_DIV_FIN: type = TokenType.SLASH; break;
            case S_GT_FIN: type = TokenType.GT; break;
            case S_GT_EQ: type = TokenType.GE; break;
            case S_LT_FIN: type = TokenType.LT; break;
            case S_LT_EQ: type = TokenType.LE; break;
            case S_NOT_EQ: type = TokenType.NEQ; break;
            case S_NOT_FIN: type = TokenType.NOT; break;
            case S_OP: type = getOpType(lexeme); break;
            default: type = TokenType.EOF;
        }

        tokens.add(new Token(type, lexeme,line));
        String record = String.format("%-5d | %-15s | %-15s | %-5s", line, lexeme, type, idx);
        tableOfSymb.add(record);
    }

    private void addToConstTable(String lexeme, String type) {
        if (!tableOfConst.containsKey(lexeme)) {
            tableOfConst.put(lexeme, type + " (idx:" + (tableOfConst.size() + 1) + ")");
        }
    }

    private TokenType getKeyword(String text) {
        switch (text) {
            case "var": return TokenType.VAR;
            case "const": return TokenType.CONST;
            case "fun": return TokenType.FUN;
            case "return": return TokenType.RETURN;
            case "if": return TokenType.IF;
            case "else": return TokenType.ELSE;
            case "while": return TokenType.WHILE;
            case "print": return TokenType.PRINT;
            case "input": return TokenType.INPUT;
            case "int": return TokenType.INT_TYPE;
            case "float": return TokenType.FLOAT_TYPE;
            case "bool": return TokenType.BOOL_TYPE;
            case "string": return TokenType.STRING_TYPE;
            case "true": return TokenType.TRUE;
            case "false": return TokenType.FALSE;
            default: return TokenType.IDENTIFIER;
        }
    }

    private TokenType getOpType(String text) {
        switch (text) {
            case "+": return TokenType.PLUS;
            case "*": return TokenType.STAR;
            case "^": return TokenType.CARET;
            case "(": return TokenType.LPAREN;
            case ")": return TokenType.RPAREN;
            case "{": return TokenType.LBRACE;
            case "}": return TokenType.RBRACE;
            case ":": return TokenType.COLON;
            case ",": return TokenType.COMMA;
            case "!": return TokenType.NOT;
            default: return TokenType.EOF;
        }
    }

    public void printTables() {
        System.out.println("\n 3.2.2 Parsing Table ");
        System.out.println(String.format("%-5s | %-15s | %-15s | %-5s", "Line", "Lexeme", "Token", "Index"));
        System.out.println("-------------------------------------------------------");
        for (String row : tableOfSymb) {
            System.out.println(row);
        }

        System.out.println("\n 3.2.3 ID Table ");
        tableOfId.forEach((k, v) -> System.out.println(k + " -> " + v));

        System.out.println("\n 3.2.4 Const Table ");
        tableOfConst.forEach((k, v) -> System.out.println(k + " -> " + v));
    }
}