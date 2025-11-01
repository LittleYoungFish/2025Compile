package frontend.parser.node.declaration;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 常量声明 ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
 */
public class ConstDecl extends Node {
    public BType type;
    public List<ConstDef> constDefs = new ArrayList<>();

    @Override
    public String getType() {
        return "ConstDecl";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        terminalConsumer.accept(new TerminalSymbol(TokenType.CONSTTK));
        type.walk(terminalConsumer, nonTerminalConsumer);
        boolean first = true;
        for (ConstDef constDef : constDefs) {
            if (first) {
                first = false;
            }else {
                terminalConsumer.accept(new TerminalSymbol(TokenType.COMMA));
            }
            constDef.walk(terminalConsumer, nonTerminalConsumer);
        }
        terminalConsumer.accept(new TerminalSymbol(TokenType.SEMICN));

        nonTerminalConsumer.accept(new NonTerminalSymbol(this));
    }
}
