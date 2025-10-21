package frontend.parser.node.function;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.node.statement.Block;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.function.Consumer;

/**
 * 函数定义 FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
 */
public class FuncDef extends Node {
    public FuncType funcType;
    public String ident;
    public FuncFParams funcFParams;
    public Block block;
    public int identLineNum = -1;

    @Override
    public String getType() {
        return "FuncDef";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        funcType.walk(terminalConsumer, nonTerminalConsumer);
        terminalConsumer.accept(new TerminalSymbol(TokenType.IDENFR, ident));
        terminalConsumer.accept(new TerminalSymbol(TokenType.LPARENT));

        if (funcFParams != null) {
            funcFParams.walk(terminalConsumer, nonTerminalConsumer);
        }

        terminalConsumer.accept(new TerminalSymbol(TokenType.RPARENT));
        block.walk(terminalConsumer, nonTerminalConsumer);

        nonTerminalConsumer.accept(new NonTerminalSymbol(this));
    }
}
