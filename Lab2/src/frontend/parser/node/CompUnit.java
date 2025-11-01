package frontend.parser.node;

import frontend.parser.node.declaration.Decl;
import frontend.parser.node.declaration.MainFuncDef;
import frontend.parser.node.function.FuncDef;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 编译单元 CompUnit → {Decl} {FuncDef} MainFuncDef
 */
public class CompUnit extends Node {
    public List<Decl> decls = new ArrayList<>(); // 可能有多个
    public List<FuncDef> funcDefs = new ArrayList<>();
    public MainFuncDef mainFuncDef;

    @Override
    public String getType() {
        return "CompUnit";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        for (Decl decl : decls) {
            // 对每个子节点遍历
            decl.walk(terminalConsumer, nonTerminalConsumer);
        }
        for (FuncDef funcDef : funcDefs) {
            funcDef.walk(terminalConsumer, nonTerminalConsumer);
        }
        mainFuncDef.walk(terminalConsumer, nonTerminalConsumer);

        nonTerminalConsumer.accept(new NonTerminalSymbol(this));
    }
}
