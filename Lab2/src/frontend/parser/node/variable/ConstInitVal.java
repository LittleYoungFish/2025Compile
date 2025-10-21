package frontend.parser.node.variable;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.node.expression.ConstExp;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 常量初值 ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}'
 */
public class ConstInitVal extends Node {
    private int UType;

    // 1. ConstInitVal → ConstExp
    public ConstExp constExp;

    // 2. ConstInitVal → '{' [ ConstExp { ',' ConstExp } ] '}'
    public List<ConstExp> constExps = new ArrayList<>();

    public ConstInitVal(int uType) {
        UType = uType;
    }

    @Override
    public String getType() {
        return "ConstInitVal";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        if(UType == 1) {
            constExp.walk(terminalConsumer, nonTerminalConsumer);
            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        } else if (UType == 2) {
            terminalConsumer.accept(new TerminalSymbol(TokenType.LBRACE));
            boolean first = true;
            for(ConstExp c : constExps) {
                if(first) {
                    first = false;
                }else {
                    terminalConsumer.accept(new TerminalSymbol(TokenType.COMMA));
                }
                c.walk(terminalConsumer, nonTerminalConsumer);
            }
            terminalConsumer.accept(new TerminalSymbol(TokenType.RBRACE));

            nonTerminalConsumer.accept(new NonTerminalSymbol(this));
        }
    }
}
