package frontend.parser.node.variable;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.node.expression.Exp;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 左值表达式 LVal → Ident ['[' Exp ']']
 */
public class LVal extends Node {
    public String ident;
    public List<Exp> dimensions = new ArrayList<>();
    public int identLineNum = -1;

    @Override
    public String getType() {
        return "LVal";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        terminalConsumer.accept(new TerminalSymbol(TokenType.IDENFR, ident));

        for(var dim : dimensions) {
            terminalConsumer.accept(new TerminalSymbol(TokenType.LBRACK));
            dim.walk(terminalConsumer, nonTerminalConsumer);
            terminalConsumer.accept(new TerminalSymbol(TokenType.RBRACK));
        }

        nonTerminalConsumer.accept(new NonTerminalSymbol(this));
    }
}
