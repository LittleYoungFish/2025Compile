package frontend.parser.node.variable;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.node.expression.Exp;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 左值表达式 LVal → Ident ['[' Exp ']']
 */
public class LVal extends Node {
    public String ident;
    public List<Exp> dimensions = new ArrayList<>();
    public int identLineNum = -1;

    @Override
    public String getType() {
        return "LVal";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        terminalConsumer.accept(new TerminalSymbol(TokenType.IDENFR, ident)); // 类型和值

        for(var dim : dimensions) {
            // accept 是 Consumer 接口的方法，用于将当前节点（或其包装的符号）传递给外部定义的操作逻辑（如打印、分析等）
            terminalConsumer.accept(new TerminalSymbol(TokenType.LBRACK));
            // walk 是 Node 类的抽象方法，每个节点（如 LVal、Exp、Number 等）都必须实现它，用于定义当前节点及其子节点的遍历规则。
            // Exp 是表达式节点（比如 1 + 2 * 3），内部可能包含运算符、操作数（Number 节点）、括号等子节点。
            // 外部操作（如打印、语义分析）需要的是这些 “拆解后的细节”（比如 1、+、2 等终端符号），而不是 Exp 节点本身。
            // Exp 的 walk 方法会递归处理其内部的子节点（比如先处理左操作数 Number，再处理运算符 +，再处理右操作数 Number）。
            // 每个子节点在 walk 中会将自己包装成 Symbol（终端或非终端），再通过 accept 传递给外部操作。
            // 如果强行用 accept 传递 dim 只能让外部操作处理 Exp 这个 “整体节点”（比如打印 Exp 类型），但无法获取 Exp 内部的运算符、操作数等细节，导致遍历不完整（子节点没有被访问）。
            dim.walk(terminalConsumer, nonTerminalConsumer); // 遍历其中所有节点
            terminalConsumer.accept(new TerminalSymbol(TokenType.RBRACK));
        }

        nonTerminalConsumer.accept(new NonTerminalSymbol(this));
    }
}
