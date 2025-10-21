package frontend.parser.node.expression;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.function.Consumer;

/**
 * 相等性表达式 EqExp → RelExp | EqExp ('==' | '!=') RelExp
 */
public class EqExp extends Node {
    private int UType;

    // 1. EqExp → RelExp
    public RelExp relExp1;
    // 2. EqExp → EqExp ('==' | '!=') RelExp
    public EqExp eqExp;
    public TokenType op;
    public RelExp relExp2;

    public EqExp(int uType) {
        UType = uType;
    }
    public int getUType() {
        return UType;
    }

    @Override
    public String getType() {
        return "EqExp";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        if(UType == 1){
            relExp1.walk(terminalConsumer, nonTerminalConsumer);
            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        } else if (UType == 2) {
            eqExp.walk(terminalConsumer, nonTerminalConsumer);
            terminalConsumer.accept(new TerminalSymbol(op));
            relExp2.walk(terminalConsumer, nonTerminalConsumer);

            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        }
    }
}
