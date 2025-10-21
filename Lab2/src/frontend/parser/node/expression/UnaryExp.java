package frontend.parser.node.expression;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.node.function.FuncRParams;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.function.Consumer;

/**
 * 一元表达式 UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
 */
public class UnaryExp extends Node {

    private int UType;

    // 1. UnaryExp → PrimaryExp
    public PrimaryExp primaryExp;

    // 2. UnaryExp → Ident '(' [FuncRParams] ')'
    public String ident;
    public FuncRParams funcRParams;
    public int identLineNum = -1;

    // 3. UnaryExp → UnaryOp UnaryExp
    public UnaryOp unaryOp;
    public UnaryExp unaryExp;

    public UnaryExp(int uType) {
        UType = uType;
    }

    @Override
    public String getType() {
        return "UnaryExp";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        if(UType == 1){
            primaryExp.walk(terminalConsumer, nonTerminalConsumer);

            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        } else if (UType == 2) {
            terminalConsumer.accept(new TerminalSymbol(TokenType.IDENFR, ident));
            terminalConsumer.accept(new TerminalSymbol(TokenType.LPARENT));

            if(funcRParams != null){
                funcRParams.walk(terminalConsumer, nonTerminalConsumer);
            }

            terminalConsumer.accept(new TerminalSymbol(TokenType.RPARENT));

            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        } else if (UType == 3) {
            unaryOp.walk(terminalConsumer, nonTerminalConsumer);
            unaryExp.walk(terminalConsumer, nonTerminalConsumer);

            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        }
    }
}
