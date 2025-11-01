package frontend.parser.symbol;

import frontend.parser.node.Node;

// 非终结符
public class NonTerminalSymbol {
    private final String type;
    private final Node node;

    public NonTerminalSymbol(Node node){
        this.type = node.getType();
        this.node = node;
    }

    public String getType(){
        return type;
    }
    public Node getNode(){
        return node;
    }

    @Override
    public String toString(){
        // <Expression>
        return "<%s>".formatted(type);
    }
}
