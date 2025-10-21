package frontend.parser.node.function;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.node.declaration.BType;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.function.Consumer;

/**
 * 函数形参 FuncFParam → BType Ident ['[' ']']
 */
public class FuncFParam extends Node {
    public BType type;
    public String ident;
    public int count = 0;//[]的计数器
    public int identLineNum = -1;

    @Override
    public String getType() {
        return "FuncFParam";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        type.walk(terminalConsumer, nonTerminalConsumer);
        terminalConsumer.accept(new TerminalSymbol(TokenType.IDENFR, ident));

        for (int i = 0; i < count; i++) {
            terminalConsumer.accept(new TerminalSymbol(TokenType.LBRACK));
            terminalConsumer.accept(new TerminalSymbol(TokenType.RBRACK));
        }

        nonTerminalConsumer.accept(new NonTerminalSymbol(this));
    }
}
