package frontend.parser.symbol;

import frontend.lexer.TokenType;

/**
 * 终结符（单词token）
 */
// Symbol（TerminalSymbol/NonTerminalSymbol）：
// 是语法树的 “处理单元”，是对 Node 或终端字符的 “包装”，包含了外部操作（如打印）需要的信息（类型、内容、行号等）。
public class TerminalSymbol {
    private final String value;
    private final TokenType type;

    public TerminalSymbol(TokenType type){
        this.type = type;
        this.value = type.toString();
    }

    // 需要有具体的值，如标识符，字符串
    public TerminalSymbol(TokenType type, String value){
        this.type = type;
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    public TokenType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "%s %s".formatted(type.name(), value);
    }

    // accept 方法是连接 “语法树节点” 和 “节点处理逻辑” 的桥梁，接收一个节点对象作为参数，然后调用 Consumer 中定义的具体操作（最后定义的打印操作）来处理这个节点。
}
