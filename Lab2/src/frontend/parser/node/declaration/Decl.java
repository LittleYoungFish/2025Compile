package frontend.parser.node.declaration;

import frontend.parser.node.Node;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.function.Consumer;

/**
 * 声明 Decl → ConstDecl | VarDecl
 */
public class Decl extends Node {
    private int UType;
    
    // 1. Decl → ConstDecl
    public ConstDecl constDecl;
    
    // 2. Decl → VarDecl
    public VarDecl varDecl;

    public Decl(int uType) {
        UType = uType;
    }

    @Override
    public String getType() {
        return "Decl";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        if(UType == 1){
            constDecl.walk(terminalConsumer, nonTerminalConsumer);
            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        } else if (UType == 2) {
            varDecl.walk(terminalConsumer, nonTerminalConsumer);
            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        }
    }
}
