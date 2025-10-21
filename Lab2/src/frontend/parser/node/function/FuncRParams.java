package frontend.parser.node.function;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.node.expression.Exp;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 函数实参表达式 FuncRParams → Exp { ',' Exp }
 */
public class FuncRParams extends Node {
    public List<Exp> exps = new ArrayList<Exp>();

    @Override
    public String getType() {
        return "FuncRParams";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        boolean first = true;

        for (Exp exp : exps) {
            if (first) {
                first = false;
            }else {
                terminalConsumer.accept(new TerminalSymbol(TokenType.COMMA));
            }
            exp.walk(terminalConsumer, nonTerminalConsumer);
        }

        nonTerminalConsumer.accept(new NonTerminalSymbol(this));
    }
}
