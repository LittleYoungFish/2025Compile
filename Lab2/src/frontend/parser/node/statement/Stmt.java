package frontend.parser.node.statement;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.node.expression.Exp;
import frontend.parser.node.variable.LVal;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 语句 Stmt → LVal '=' Exp ';'
 * | [Exp] ';'
 * | Block
 * | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
 * | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
 * | 'break' ';'
 * | 'continue' ';'
 * | 'return' [Exp] ';'
 * | 'printf''('StringConst {','Exp}')'';'
 */
public class Stmt extends Node {
    private int UType;

    // 1. Stmt → LVal '=' Exp ';'
    public LVal lVal;
    public Exp exp1;
    // 2. Stmt → [Exp] ';'
    public Exp exp2;
    // 3. Stmt → Block
    public Block block;
    // 4. Stmt → 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    public Cond cond1;
    public Stmt ifStmt;
    public Stmt elseStmt;
    // 5. Stmt -> 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
    public ForStmt forStmt1;
    public Cond cond2;
    public ForStmt forStmt2;
    public Stmt stmt;
    // 6. Stmt -> 'break' ';' Stmt -> 'continue' ';'
    public TokenType type;
    public int tkLineNum = -1;
    // 7. Stmt -> 'return' [Exp] ';'
    public Exp exp3;
    public int returnLineNum = -1;
    // 8. Stmt -> 'printf''('StringConst {','Exp}')'';'
    public String stringConst;
    public List<Exp> exps = new ArrayList<>();
    public int printfLineNum = -1;

    public Stmt(int uType) {
        UType = uType;
    }
    public int getUType() {
        return UType;
    }

    @Override
    public String getType() {
        return "Stmt";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        if(UType == 1){
            lVal.walk(terminalConsumer, nonTerminalConsumer);
            terminalConsumer.accept(new TerminalSymbol(TokenType.ASSIGN));
            exp1.walk(terminalConsumer, nonTerminalConsumer);
            terminalConsumer.accept(new TerminalSymbol(TokenType.SEMICN));

            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        }else if(UType == 2){
            if(exp2 != null){
                exp2.walk(terminalConsumer, nonTerminalConsumer);
            }
            terminalConsumer.accept(new TerminalSymbol(TokenType.SEMICN));

            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        }else if(UType == 3){
            block.walk(terminalConsumer, nonTerminalConsumer);
            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        }else if(UType == 4){
            terminalConsumer.accept(new TerminalSymbol(TokenType.IFTK));
            terminalConsumer.accept(new TerminalSymbol(TokenType.LPARENT));

            cond1.walk(terminalConsumer, nonTerminalConsumer);

            terminalConsumer.accept(new TerminalSymbol(TokenType.RPARENT));

            ifStmt.walk(terminalConsumer, nonTerminalConsumer);

            if (elseStmt != null) {
                terminalConsumer.accept(new TerminalSymbol(TokenType.ELSETK));
                elseStmt.walk(terminalConsumer, nonTerminalConsumer);
            }

            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        } else if (UType == 5) {
            terminalConsumer.accept(new TerminalSymbol(TokenType.FORTK));
            terminalConsumer.accept(new TerminalSymbol(TokenType.LPARENT));
            if (forStmt1 != null) {
                forStmt1.walk(terminalConsumer, nonTerminalConsumer);
            }
            terminalConsumer.accept(new TerminalSymbol(TokenType.SEMICN));
            if (cond2 != null) {
                cond2.walk(terminalConsumer, nonTerminalConsumer);
            }
            terminalConsumer.accept(new TerminalSymbol(TokenType.SEMICN));
            if (forStmt2 != null) {
                forStmt2.walk(terminalConsumer, nonTerminalConsumer);
            }
            terminalConsumer.accept(new TerminalSymbol(TokenType.RPARENT));
            stmt.walk(terminalConsumer, nonTerminalConsumer);

            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        } else if (UType == 6) {
            terminalConsumer.accept(new TerminalSymbol(type));
            terminalConsumer.accept(new TerminalSymbol(TokenType.SEMICN));

            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        } else if (UType == 7) {
            terminalConsumer.accept(new TerminalSymbol(TokenType.RETURNTK));

            if (exp3 != null) {
                exp3.walk(terminalConsumer, nonTerminalConsumer);
            }

            terminalConsumer.accept(new TerminalSymbol(TokenType.SEMICN));

            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        } else if (UType == 8) {
            terminalConsumer.accept(new TerminalSymbol(TokenType.PRINTFTK));
            terminalConsumer.accept(new TerminalSymbol(TokenType.LPARENT));
            terminalConsumer.accept(new TerminalSymbol(TokenType.STRCON, stringConst));
            for (var exp : exps) {
                terminalConsumer.accept(new TerminalSymbol(TokenType.COMMA));
                exp.walk(terminalConsumer, nonTerminalConsumer);
            }
            terminalConsumer.accept(new TerminalSymbol(TokenType.RPARENT));
            terminalConsumer.accept(new TerminalSymbol(TokenType.SEMICN));

            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        }
    }
}
