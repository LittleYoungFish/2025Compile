package frontend.parser.node.expression;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.function.Consumer;

/**
 * 逻辑与表达式 LAndExp → EqExp | LAndExp '&&' EqExp
 */
public class LAndExp extends Node {
    private int UType;

    // 1. LAndExp → EqExp
    public EqExp eqExp1;
    // 2. LAndExp → LAndExp '&&' EqExp
    public LAndExp lAndExp;
    public EqExp eqExp2;

    public LAndExp(int uType) {
        UType = uType;
    }
    public int getUType() {
        return UType;
    }

    @Override
    public String getType() {
        return "LAndExp";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        if(UType == 1){
            eqExp1.walk(terminalConsumer, nonTerminalConsumer);
            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        } else if (UType == 2) {
            lAndExp.walk(terminalConsumer, nonTerminalConsumer);
            terminalConsumer.accept(new TerminalSymbol(TokenType.AND));
            eqExp2.walk(terminalConsumer, nonTerminalConsumer);

            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        }
    }
}
