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

    // 读取下一个token
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
    public Token readTokenByOffset(int offset) {
        assert offset < tokenBufLen;
        return tokenBuf[(currTokenPos + offset) % tokenBufLen];
    }

    // 查找token，在缓冲中查找目标令牌find，如果遇到until则停止查找
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
