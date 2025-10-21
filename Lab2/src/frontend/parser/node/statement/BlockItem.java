package frontend.parser.node.statement;

import frontend.parser.node.Node;
import frontend.parser.node.declaration.Decl;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.function.Consumer;

/**
 * 语句块项 BlockItem → Decl | Stmt
 */
public class BlockItem extends Node {
    private int UType;

    // 1. BlockItem → Decl
    public Decl decl;
    // 2. BlockItem → Stmt
    public Stmt stmt;

    public BlockItem(int uType) {
        UType = uType;
    }
    public int getUType() {
        return UType;
    }

    @Override
    public String getType() {
        return "BlockItem";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        if(UType == 1){
            decl.walk(terminalConsumer, nonTerminalConsumer);
            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        } else if (UType == 2) {
            stmt.walk(terminalConsumer, nonTerminalConsumer);
            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        }
    }
}
