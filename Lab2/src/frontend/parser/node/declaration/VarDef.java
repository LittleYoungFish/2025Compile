package frontend.parser.node.declaration;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.node.expression.ConstExp;
import frontend.parser.node.variable.InitVal;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 变量定义 VarDef → Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '=' InitVal
 */
public class VarDef extends Node {
    public String ident;
    public List<ConstExp> dimensions = new ArrayList<>();
    public InitVal initVal;
    public int identLineNum = -1;

    @Override
    public String getType() {
        return "VarDef";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        terminalConsumer.accept(new TerminalSymbol(TokenType.IDENFR, ident));

        for (ConstExp dimension : dimensions) {
            terminalConsumer.accept(new TerminalSymbol(TokenType.LBRACK));
            dimension.walk(terminalConsumer, nonTerminalConsumer);
            terminalConsumer.accept(new TerminalSymbol(TokenType.RBRACK));
        }

        if (initVal != null) {
            terminalConsumer.accept(new TerminalSymbol(TokenType.ASSIGN));
            initVal.walk(terminalConsumer, nonTerminalConsumer);
        }

        nonTerminalConsumer.accept(new NonTerminalSymbol(this));
    }
}
