package frontend.parser.node.expression;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.function.Consumer;

/**
 * 单目运算符 UnaryOp → '+' | '−' | '!' 注：'!'仅出现在条件表达式中
 */
public class UnaryOp extends Node {
    public TokenType op;

    @Override
    public String getType() {
        return "UnaryOp";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        terminalConsumer.accept(new TerminalSymbol(op));

        nonTerminalConsumer.accept(new NonTerminalSymbol(this));
    }
}
