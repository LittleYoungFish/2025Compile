package frontend.parser;

import frontend.lexer.Token;
import frontend.parser.node.Node;

public class ParseResult {
    private final Token nextToken;
    private final Node subtree;

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
