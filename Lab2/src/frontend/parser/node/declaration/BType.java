package frontend.parser.node.declaration;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.function.Consumer;

/**
 * 基本类型 BType -> ‘int’
 */
public class BType extends Node {
    public TokenType type = TokenType.INTTK;

    @Override
    public String getType() {
        return "BType";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        terminalConsumer.accept(new TerminalSymbol(type));
        nonTerminalConsumer.accept(new NonTerminalSymbol(this));
    }
}
