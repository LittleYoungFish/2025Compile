package frontend.parser.node.declaration;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.node.expression.ConstExp;
import frontend.parser.node.variable.ConstInitVal;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 常量定义 ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
 */
public class ConstDef extends Node {
    public String ident;
    public List<ConstExp> dimensions = new ArrayList<>(); //可能有多维度
    public ConstInitVal constInitVal;
    public int identLineNum = -1;

    @Override
    public String getType() {
        return "ConstDef";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        terminalConsumer.accept(new TerminalSymbol(TokenType.IDENFR, ident));

        for(var dimension : dimensions){
            // [
            terminalConsumer.accept(new TerminalSymbol(TokenType.LBRACK));

            dimension.walk(terminalConsumer, nonTerminalConsumer);

            // ]
            terminalConsumer.accept(new TerminalSymbol(TokenType.RBRACK));
        }

        terminalConsumer.accept(new TerminalSymbol(TokenType.ASSIGN));
        constInitVal.walk(terminalConsumer, nonTerminalConsumer);
        nonTerminalConsumer.accept(new NonTerminalSymbol(this));
    }
}
