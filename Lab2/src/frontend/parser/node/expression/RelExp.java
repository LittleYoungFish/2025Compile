package frontend.parser.node.expression;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.function.Consumer;

/**
 * 关系表达式 RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
 */
public class RelExp extends Node {
    private int UType;
    
    // 1. RelExp → AddExp
    public AddExp addExp1;
    // 2. RelExp → RelExp ('<' | '>' | '<=' | '>=') AddExp
    public RelExp relExp;
    public TokenType op;
    public AddExp addExp2;
    
    public RelExp(int uType) {
        UType = uType;
    }
    public int getUType() {
        return UType;
    }
    
    @Override
    public String getType() {
        return "RelExp";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        if(UType == 1){
            addExp1.walk(terminalConsumer, nonTerminalConsumer);
            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        } else if (UType == 2) {
            relExp.walk(terminalConsumer, nonTerminalConsumer);
            terminalConsumer.accept(new TerminalSymbol(op));
            addExp2.walk(terminalConsumer, nonTerminalConsumer);

            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        }
    }
}
