package frontend.parser;

import frontend.lexer.Token;
import frontend.parser.node.Node;

public class ParseResult {
    private final Token nextToken; // 解析完该节点后下一个Token
    private final Node subtree; // 解析后的语法树

    public ParseResult(Token nextToken, Node subtree) {
        this.nextToken = nextToken;
        this.subtree = subtree;
    }

    public Token getNextToken() {
        return nextToken;
    }

    public Node getSubtree() {
        return subtree;
    }
}
