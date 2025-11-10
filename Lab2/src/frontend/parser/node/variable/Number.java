package frontend.parser.node.variable;

import frontend.lexer.TokenType;
import frontend.parser.node.Node;
import frontend.parser.symbol.NonTerminalSymbol;
import frontend.parser.symbol.TerminalSymbol;

import java.util.function.Consumer;

/**
 * 数值 Number → IntConst
 */
public class Number extends Node {
    public String intConst;

    @Override
    public String getType() {
        return "Number";
    }

    @Override
    public void walk(Consumer<TerminalSymbol> terminalConsumer, Consumer<NonTerminalSymbol> nonTerminalConsumer) {
        // accept 方法是连接 “语法树节点” 和 “节点处理逻辑” 的桥梁，接收一个节点对象作为参数，然后调用 Consumer 中定义的具体操作来处理这个节点。
        // 语法分析中终结符号在walk里输出
        terminalConsumer.accept(new TerminalSymbol(TokenType.INTCON, intConst));
        nonTerminalConsumer.accept(new NonTerminalSymbol(this));
    }
}
