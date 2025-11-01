package frontend.parser.node.expression;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.function.Consumer;

/**
 * 加减表达式 AddExp → MulExp | AddExp ('+' | '−') MulExp
 */
public class AddExp extends Node {

    private int UType;

    // 1. AddExp → MulExp
    public MulExp mulExp1;

    // 2. AddExp → AddExp ('+' | '−') MulExp
    public AddExp addExp;
    public TokenType op; // 需要一个type来识别是哪个操作符
    public MulExp mulExp2;

    public AddExp(int uType) {
        UType = uType;
    }

    @Override
    public String getType(){
        return "AddExp";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        if(this.UType == 1){
            mulExp1.walk(terminalConsumer, nonTerminalConsumer);
            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        } else if (this.UType == 2) {
            addExp.walk(terminalConsumer, nonTerminalConsumer);
            terminalConsumer.accept(new TerminalSymbol(op));
            mulExp2.walk(terminalConsumer, nonTerminalConsumer);

            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        }
    }
}
