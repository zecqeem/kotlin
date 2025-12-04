grammar Gorb;


// --- PARSER RULES ---

program : statement* EOF;

statement
    : varDecl
    | constDecl
    | funDecl
    | assignStmt
    | printStmt
    | inputStmt
    | ifStmt
    | whileStmt
    | returnStmt
    | block
    ;

block : LBRACE statement* RBRACE;

varDecl : VAR ID COLON type (ASSIGN expression)?;
constDecl : CONST ID COLON type ASSIGN expression;

funDecl : FUN ID LPAREN paramList? RPAREN ARROW type block;
paramList : parameter (COMMA parameter)*;
parameter : ID COLON type;
returnStmt : RETURN expression;

assignStmt : ID ASSIGN expression;
printStmt : PRINT LPAREN expression RPAREN;
inputStmt : INPUT LPAREN ID RPAREN;

type : INT_TYPE | FLOAT_TYPE | BOOL_TYPE | STRING_TYPE;

ifStmt : IF LPAREN expression RPAREN statement (ELSE statement)?;
whileStmt : WHILE LPAREN expression RPAREN statement;

// --- EXPRESSIONS ---

expression : relationalExpr;

relationalExpr : addSubExpr ( (LT|GT|EQ_EQ|NEQ|LE|GE) addSubExpr )* ;

addSubExpr : multiDivExpr ( (PLUS|MINUS) multiDivExpr )* ;

multiDivExpr : powerExpr ( (STAR|DIV) powerExpr )* ;

powerExpr : primaryExpr (CARET powerExpr)? ;

primaryExpr
    : ID LPAREN expressionList? RPAREN
    | ID
    | BOOL
    | INT
    | FLOAT
    | STRING
    | LPAREN expression RPAREN
    ;

expressionList : expression (COMMA expression)*;

// --- LEXER RULES ---

VAR : 'var';
CONST : 'const';
FUN : 'fun';
RETURN : 'return';
PRINT : 'print';
INPUT : 'input';
IF : 'if';
ELSE : 'else';
WHILE : 'while';

INT_TYPE : 'int';
FLOAT_TYPE : 'float';
BOOL_TYPE : 'bool';
STRING_TYPE : 'string';

BOOL : 'true' | 'false';
FLOAT : [0-9]+ '.' [0-9]+;
INT : [0-9]+;
STRING : '"' (~[\r\n])* '"';

PLUS : '+';
MINUS : '-';
STAR : '*';
DIV : '/';
CARET : '^';
ASSIGN : '=';
ARROW : '->';
COLON : ':';

EQ_EQ : '==';
NEQ : '!=';
LE : '<=';
GE : '>=';
LT : '<';
GT : '>';

LPAREN : '(';
RPAREN : ')';
LBRACE : '{';
RBRACE : '}';
COMMA : ',';

ID : [a-zA-Z_][a-zA-Z0-9_]*;

WS : [ \t\r\n]+ -> skip;
LINE_COMMENT : '//' ~[\r\n]* -> skip;