package frontend.parser.node.declaration;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.node.statement.Block;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.function.Consumer;

/**
 * 主函数定义 MainFuncDef → 'int' 'main' '(' ')' Block
 */
public class MainFuncDef extends Node {
    public Block block;

    @Override
    public String getType() {
        return "MainFuncDef";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        terminalConsumer.accept(new TerminalSymbol(TokenType.INTTK));
        terminalConsumer.accept(new TerminalSymbol(TokenType.MAINTK));
        terminalConsumer.accept(new TerminalSymbol(TokenType.LPARENT));
        terminalConsumer.accept(new TerminalSymbol(TokenType.RPARENT));

        block.walk(terminalConsumer, nonTerminalConsumer);

        nonTerminalConsumer.accept(new NonTerminalSymbol(this));
    }
}
