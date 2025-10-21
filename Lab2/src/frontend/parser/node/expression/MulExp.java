package frontend.parser.node.expression;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.function.Consumer;

/**
 * 乘除模表达式 MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
 */
public class MulExp extends Node {

    private int UType;

    // 1. MulExp → UnaryExp
    public UnaryExp unaryExp1;

    // 2. MulExp → MulExp ('*' | '/' | '%') UnaryExp
    public MulExp mulExp;
    public TokenType op;
    public UnaryExp unaryExp2;

    public MulExp(int uType) {
        UType = uType;
    }

    @Override
    public String getType() {
        return "MulExp";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        if(UType == 1){
            unaryExp1.walk(terminalConsumer, nonTerminalConsumer);

            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        } else if (UType == 2) {
            mulExp.walk(terminalConsumer, nonTerminalConsumer);
            terminalConsumer.accept(new TerminalSymbol(op));
            unaryExp2.walk(terminalConsumer, nonTerminalConsumer);

            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        }
    }
}
