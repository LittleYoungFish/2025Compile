package frontend.parser.symbol;

import frontend.lexer.TokenType;

/**
 * 终结符（单词token）
 */
public class TerminalSymbol {
    private final String value;
    private final TokenType type;

    public TerminalSymbol(TokenType type){
        this.type = type;
        this.value = type.toString();
    }

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
}
