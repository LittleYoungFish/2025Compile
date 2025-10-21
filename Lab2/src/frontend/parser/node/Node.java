package frontend.parser.node;

import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.function.Consumer;

public abstract class Node {
    public abstract String getType();

    public abstract void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer);
}
