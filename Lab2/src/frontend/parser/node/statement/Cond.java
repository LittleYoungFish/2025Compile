package frontend.parser.node.statement;

import frontend.parser.node.Node;
import frontend.parser.node.expression.LOrExp;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.function.Consumer;

/**
 * 条件表达式 Cond → LOrExp
 */
public class Cond extends Node {
    public LOrExp lOrExp;

    @Override
    public String getType() {
        return "Cond";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        lOrExp.walk(terminalConsumer, nonTerminalConsumer);
        nonTerminalConsumer.accept(new NonTerminalSymbol(this));
    }
}
