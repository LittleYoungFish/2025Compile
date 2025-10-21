package frontend.lexer;

import java.util.ArrayList;
import java.util.List;

public class TokenList {
    private List<Token> tokenList = new ArrayList<>();

    public void addToken(Token token) {
        tokenList.add(token);
    }

    public List<Token> getTokenList() {
        return tokenList;
    }
}
