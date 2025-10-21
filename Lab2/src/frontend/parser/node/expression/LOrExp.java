package frontend.parser.node.expression;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.function.Consumer;

/**
 * 逻辑或表达式 LOrExp → LAndExp | LOrExp '||' LAndExp
 */
public class LOrExp extends Node {
    private int UType;

    // 1. LOrExp → LAndExp
    public LAndExp lAndExp1;
    // 2. LOrExp → LOrExp '||' LAndExp
    public LOrExp lOrExp;
    public LAndExp lAndExp2;

    public LOrExp(int uType) {
        UType = uType;
    }
    public int getUType() {
        return UType;
    }

    @Override
    public String getType() {
        return "LOrExp";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        if(UType == 1){
            lAndExp1.walk(terminalConsumer, nonTerminalConsumer);
            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        } else if (UType == 2) {
            lOrExp.walk(terminalConsumer, nonTerminalConsumer);
            terminalConsumer.accept(new TerminalSymbol(TokenType.OR));
            lAndExp2.walk(terminalConsumer, nonTerminalConsumer);

            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        }
    }
}
