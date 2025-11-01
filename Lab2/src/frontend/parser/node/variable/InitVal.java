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
 * 变量初值 InitVal → Exp | '{' [ Exp { ',' Exp } ] '}'
 */
public class InitVal extends Node {
    private int UType;

    // 1. InitVal → Exp
    public Exp exp;

    // 2. InitVal → '{' [ Exp { ',' Exp } ] '}'
    public List<Exp> exps = new ArrayList<>();

    public InitVal(int uType) {
        UType = uType;
    }

    @Override
    public String getType() {
        return "InitVal";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        if (UType == 1) {
            exp.walk(terminalConsumer, nonTerminalConsumer);
            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        } else if (UType == 2) {
            terminalConsumer.accept(new TerminalSymbol(TokenType.LBRACE));

            boolean first = true;
            // exps为空则不会进入
            for (Exp exp : exps) {
                if (first) {
                    first = false;
                }else {
                    // 先处理逗号,
                    terminalConsumer.accept(new TerminalSymbol(TokenType.COMMA));
                }
                exp.walk(terminalConsumer, nonTerminalConsumer);
            }
            terminalConsumer.accept(new TerminalSymbol(TokenType.RBRACE));

            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        }
    }
}
