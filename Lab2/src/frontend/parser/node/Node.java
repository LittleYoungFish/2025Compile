package frontend.parser.node;

import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.function.Consumer;

// Node（如 Exp、LVal）：是语法树的 “结构节点”，代表语法规则（如 Exp → 数字 + 数字），
// 可能包含多个子节点（子 Node），本身不是 “可直接处理的最小单元”。
public abstract class Node {
    public abstract String getType();

    public abstract void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer);
}
