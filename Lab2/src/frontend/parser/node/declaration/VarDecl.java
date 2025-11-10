package frontend.parser.node.declaration;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 变量声明 VarDecl → [ 'static' ] BType VarDef { ',' VarDef } ';'
 */
public class VarDecl extends Node {
    private int UType;

    public BType type;
    public List<VarDef> varDefs = new ArrayList<>();

    public VarDecl(){

    }

    public VarDecl(int uType) {
        UType = uType;
    }

    public int getUType() {
        return UType;
    }

    @Override
    public String getType() {
        return "VarDecl";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        // 1:含static，先处理static，后续一致
        if (UType == 1) {
            terminalConsumer.accept(new TerminalSymbol(TokenType.STATICTK));
        }
        type.walk(terminalConsumer, nonTerminalConsumer);
        boolean first = true;
        for (VarDef varDef : varDefs) {
            if (first) {
                first = false;
            }else {
                terminalConsumer.accept(new TerminalSymbol(TokenType.COMMA));
            }
            varDef.walk(terminalConsumer, nonTerminalConsumer);
        }
        terminalConsumer.accept(new TerminalSymbol(TokenType.SEMICN));

        nonTerminalConsumer.accept(new NonTerminalSymbol(this));
    }
}
