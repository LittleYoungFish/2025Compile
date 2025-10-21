package frontend.parser.node.expression;

import frontend.parser.node.Node;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.function.Consumer;

/**
 * 常量表达式 ConstExp → AddExp 注：使用的 Ident 必须是常量
 */
public class ConstExp extends Node {
    public AddExp addExp;

    @Override
    public String getType() {
        return "ConstExp";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        addExp.walk(terminalConsumer, nonTerminalConsumer);

        nonTerminalConsumer.accept(new NonTerminalSymbol(this));
    }
}
