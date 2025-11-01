package frontend.parser.node.statement;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.node.expression.Exp;
import frontend.parser.node.variable.LVal;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 语句 ForStmt → LVal '=' Exp { ',' LVal '=' Exp }
 */
public class ForStmt extends Node {
    public List<LVal> lVals = new ArrayList<>();
    public List<Exp> exps = new ArrayList<>();

    @Override
    public String getType() {
        return "ForStmt";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        // 处理第一个 LVal = Exp 一定有一个
        lVals.get(0).walk(terminalConsumer, nonTerminalConsumer);
        terminalConsumer.accept(new TerminalSymbol(TokenType.ASSIGN));
        exps.get(0).walk(terminalConsumer, nonTerminalConsumer);

        // 处理剩余的 ", LVal = Exp"（如果有）
        for (int i = 1; i < lVals.size(); i++) {
            terminalConsumer.accept(new TerminalSymbol(TokenType.COMMA));
            lVals.get(i).walk(terminalConsumer, nonTerminalConsumer);
            terminalConsumer.accept(new TerminalSymbol(TokenType.ASSIGN));
            exps.get(i).walk(terminalConsumer, nonTerminalConsumer);
        }

        nonTerminalConsumer.accept(new NonTerminalSymbol(this));
    }
}
