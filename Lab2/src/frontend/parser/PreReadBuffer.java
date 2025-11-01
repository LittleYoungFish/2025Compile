package frontend.parser;

import exception.LexerException;
import frontend.lexer.Lexer;
import frontend.lexer.Token;
import frontend.lexer.TokenType;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

public class PreReadBuffer {
    private final Lexer lexer;
    private final int tokenBufLen; // 缓冲池大小，决定最多能预读多少个token
    private final Token[] tokenBuf; // 缓冲数组，存储预读的token
    private int currTokenPos = 0; // 当前token位置指针，指向正在处理的token
    private final Queue<Token> findBuffer = new ArrayDeque<>(); // 额外的查找缓冲，用于暂存超出缓冲池大小的token
    private Token preToken = null; // 记录上一个处理过的token

    // 初始化，提前读取bufLen-1个令牌，存入tokenBuf
    public PreReadBuffer(Lexer lexer, int bufLen) throws LexerException, IOException {
        assert bufLen >= 2;

        this.lexer = lexer;
        this.tokenBufLen = bufLen;
        this.tokenBuf = new Token[this.tokenBufLen];

        for (int i = 1; i < this.tokenBufLen; i++) {
            if (this.lexer.next()) {
                this.tokenBuf[i] = lexer.getToken();
            } else {
                this.tokenBuf[i] = new Token("EOF", null, 0);
            }
        }
    }

    // 读取下一个token（会推进解析位置）
    // 从缓冲中读取 “当前要处理的下一个 Token”，并更新缓冲状态，是解析器获取 Token 的主要入口。
    public Token readNextToken() throws LexerException, IOException {
        preToken = tokenBuf[currTokenPos]; //记录当前token为上一个处理的token，即preToken

        if (!findBuffer.isEmpty()) { // 从findBuffer读取新token
            tokenBuf[currTokenPos] = findBuffer.poll();
        } else if (lexer.next()) { //从lexer读取新token
            tokenBuf[currTokenPos] = lexer.getToken();
        } else {
            tokenBuf[currTokenPos] = new Token("EOF", null, 0);  // token not null
        }
        currTokenPos = (currTokenPos + 1) % tokenBufLen;
        return tokenBuf[currTokenPos];
    }

    //按偏移量读取token
    // 当前 Token 对应多条语法规则，且这些规则的 “公共前缀” 仅靠当前 Token 无法区分时，必须预读后续 Token 才能确定具体要解析的产生式。
    // 读取 “当前 Token 之后第 offset 个 Token”（不推进解析指针），用于解决 “短歧义”（仅需预读 1~2 个 Token 即可区分语法规则）。
    public Token readTokenByOffset(int offset) {
        assert offset < tokenBufLen;
        return tokenBuf[(currTokenPos + offset) % tokenBufLen];
    }

    // 查找token，在缓冲中查找目标令牌find，如果遇到until则停止查找
    // 从当前位置开始，向后查找目标 Token（find），直到遇到终止 Token（until）为止，用于解决 “长歧义”（需要预读超过缓冲池大小的 Token 才能判断）。
    public boolean findUntil(TokenType find, TokenType until) throws LexerException, IOException {
        for (int i = 0, j = currTokenPos; i < tokenBufLen; i++, j = (currTokenPos + 1) % tokenBufLen) {
            if (tokenBuf[j].getTokenType() == find) {
                return true;
            } else if (tokenBuf[j].getTokenType() == until) {
                return false;
            }
        }

        while (lexer.next()) {
            Token token = lexer.getToken();
            findBuffer.add(lexer.getToken());
            if (token.getTokenType() == find) {
                return true;
            } else if (token.getTokenType() == until) {
                return false;
            }
        }
        return false;
    }

    // 获取上一个处理过的token，用于上下文判断
    public Token readPreToken() {
        return preToken;
    }
}
