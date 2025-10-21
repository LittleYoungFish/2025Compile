package frontend.parser.node.function;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 函数形参表 FuncFParams → FuncFParam { ',' FuncFParam }
 */
public class FuncFParams extends Node {
    public List<FuncFParam> funcFParams = new ArrayList<>();

    @Override
    public String getType() {
        return "FuncFParams";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        boolean first = true;
        for (FuncFParam param : funcFParams) {
            if (first) {
                first = false;
            }else {
                terminalConsumer.accept(new TerminalSymbol(TokenType.COMMA));
            }
            param.walk(terminalConsumer, nonTerminalConsumer);
        }

        nonTerminalConsumer.accept(new NonTerminalSymbol(this));
    }
}
