package frontend;

import java.util.ArrayList;
import java.util.List;

/**
 * Token类
 */
public class Token {
    private final String tokenContent; //具体内容
    private final TokenType tokenType; //token类型
    private final int lineNum; //所在行号

    public Token(String tokenContent, TokenType tokenType, int lineNum) {
        this.tokenContent = tokenContent;
        this.tokenType = tokenType;
        this.lineNum = lineNum;
    }

    public String getTokenContent() {
        return tokenContent;
    }
    public TokenType getTokenType() {
        return tokenType;
    }
    public int getLineNum() {
        return lineNum;
    }
}
