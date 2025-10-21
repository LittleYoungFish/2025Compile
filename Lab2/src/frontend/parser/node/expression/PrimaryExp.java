package frontend.parser.node.expression;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.node.variable.LVal;
import frontend.parser.node.variable.Number;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.function.Consumer;

/**
 * 基本表达式 PrimaryExp → '(' Exp ')' | LVal | Number
 */
public class PrimaryExp extends Node {
    private int UType;

    // 1. PrimaryExp → '(' Exp ')'
    public Exp exp;

    // 2. PrimaryExp → LVal
    public LVal lVal;

    // 3. PrimaryExp → Number
    public Number number;

    public PrimaryExp(int uType) {
        UType = uType;
    }

    @Override
    public String getType() {
        return "PrimaryExp";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        if(UType == 1){
            terminalConsumer.accept(new TerminalSymbol(TokenType.LPARENT));
            exp.walk(terminalConsumer, nonTerminalConsumer);
            terminalConsumer.accept(new TerminalSymbol(TokenType.RPARENT));

            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        } else if (UType == 2) {
            lVal.walk(terminalConsumer, nonTerminalConsumer);

            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        } else if (UType == 3) {
            number.walk(terminalConsumer, nonTerminalConsumer);

            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        }
    }
}
