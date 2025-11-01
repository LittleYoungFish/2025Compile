package frontend.parser.node.statement;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 语句块 Block → '{' { BlockItem } '}'
 */
public class Block extends Node {
    public List<BlockItem> blockItems = new ArrayList<>();
    public int blockRLineNum = -1; // 右花括号位置

    @Override
    public String getType() {
        return "Block";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        terminalConsumer.accept(new TerminalSymbol(TokenType.LBRACE));
        for(BlockItem blockItem : blockItems){
            blockItem.walk(terminalConsumer, nonTerminalConsumer);
        }
        terminalConsumer.accept(new TerminalSymbol(TokenType.RBRACE));

        nonTerminalConsumer.accept(new NonTerminalSymbol(this));
    }

    public boolean isWithoutReturn(){
        if(blockItems.isEmpty()){
            return true;
        }
        BlockItem blockItem = blockItems.get(blockItems.size() - 1);

        return !(blockItem.getUType() == 2)
                || !(blockItem.stmt.getUType() == 7);
    }
}
