package frontend.parser.node.variable;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.function.Consumer;

/**
 * 数值 Number → IntConst
 */
public class Number extends Node {
    public String intConst;

    @Override
    public String getType() {
        return "Number";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        terminalConsumer.accept(new TerminalSymbol(TokenType.INTCON, intConst));
        nonTerminalConsumer.accept(new NonTerminalSymbol(this));
    }
}
