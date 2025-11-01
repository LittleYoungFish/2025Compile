package frontend.parser.node.function;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.function.Consumer;

/**
 * 函数类型 FuncType → 'void' | 'int'
 */
public class FuncType extends Node {
    public TokenType type;

    @Override
    public String getType() {
        return "FuncType";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        terminalConsumer.accept(new TerminalSymbol(type)); // 和type有关
        nonTerminalConsumer.accept(new NonTerminalSymbol(this));
    }
}
