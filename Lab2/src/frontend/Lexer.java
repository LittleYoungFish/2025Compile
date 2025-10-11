package frontend;

import error.ErrorRecorder;
import error.ErrorType;
import exception.LexerException;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

public class Lexer {
    private final Reader reader; //读取
    private final ErrorRecorder errorRecorder; //错误记录
    private int lineNum = 1; //当前行号
    private int readIndex; // 主指针
    private String code; //源代码
    private Token token = null; //当前的token

    private boolean hasBackChar = false; //是否需要回退字符
    private int backChar; // 需要回退的字符

    private final int EOF = -1;

    // 关键字映射表
    private final static Map<String, TokenType> keyWordTable = Map.ofEntries(
            Map.entry("const", TokenType.CONSTTK),
            Map.entry("int", TokenType.INTTK),
            Map.entry("static", TokenType.STATICTK),
            Map.entry("break", TokenType.BREAKTK),
            Map.entry("continue", TokenType.CONTINUETK),
            Map.entry("if", TokenType.IFTK),
            Map.entry("main", TokenType.MAINTK),
            Map.entry("else", TokenType.ELSETK),
            Map.entry("for", TokenType.FORTK),
            Map.entry("return", TokenType.RETURNTK),
            Map.entry("void", TokenType.VOIDTK),
            Map.entry("printf", TokenType.PRINTFTK)
    );

    public Lexer(Reader reader, ErrorRecorder errorRecorder) {
        this.reader = reader;
        this.errorRecorder = errorRecorder;
    }

    public boolean next() throws IOException, LexerException {
        TokenType tokenType; //token类型
        String content; //token的内容
        StringBuilder builder = new StringBuilder();
        int c = getChar();
        builder.append((char) c); //先加进去builder

        //关键字或标识符
        if(isLetter(c)){
            c = getChar();
            while(isLetter(c) || isDigit(c)){
                builder.append((char) c);
                c = getChar();
            }
            //不符合，回退
            backChar(c);
            content = builder.toString();//具体内容
            // 是否为关键字，否则为标识符
            tokenType = keyWordTable.getOrDefault(content, TokenType.IDENFR);
            token = new Token(content, tokenType, lineNum);
        } else if (isPositiveDigit(c)) {// 数字
            c = getChar();
            while(isDigit(c)){
                builder.append((char) c);
                c = getChar();
            }
            backChar(c);//不再是数字，回退
            content = builder.toString();
            tokenType = TokenType.INTCON;
            token = new Token(content, tokenType, lineNum);
        } else if (c == '0') { // 数字0
            content = builder.toString(); // 0
            tokenType = TokenType.INTCON;
            token = new Token(content, tokenType, lineNum);
        } else if (c == '\"') { //字符串常量
            c = getChar();
            while (c != EOF && c != '\"'){
                builder.append((char) c);
                //“”中只能出现字符串或者%d或者\n
                if(c == '%'){
                    c = getChar();
                    if (c == 'd') {
                        builder.append((char) c);
                    }else { //不合法
                        builder.append((char) c);
                        //errorRecorder.addError(ErrorType.ILLEGAL_SYMBOL, lineNum);
                    }
                    if(c == '\"'){
                        break; //字符串结束
                    }
                } else if (c == '\\') {
                    c = getChar();
                    if (c == 'n') {
                        builder.append((char) c);
                    }else {
                        builder.append((char) c);
                        //errorRecorder.addError(ErrorType.ILLEGAL_SYMBOL, lineNum);
                    }
                    if(c == '\"'){
                        break;
                    }
                } else if (!isNormalLetter(c)) {
                    //errorRecorder.addError(ErrorType.ILLEGAL_SYMBOL, lineNum);
                }
                c= getChar();
            }
            if(c == '\"'){ //右双引号（字符串结束）
                builder.append((char) c);
                content = builder.toString();
                tokenType = TokenType.STRCON;
                token = new Token(content, tokenType, lineNum);
            }
            //其他情况不合法
        } else if (c == '!') {
            c = getChar(); // 判断下一个是否为 =
            if(c == '='){
                builder.append((char) c);
                content = builder.toString();
                tokenType = TokenType.NEQ; // !=
                token = new Token(content, tokenType, lineNum);
            }else {
                backChar(c);//回退
                content = builder.toString();
                tokenType = TokenType.NOT; // !
                token = new Token(content, tokenType, lineNum);
            }
        } else if (c == '&') {
            c = getChar(); // 判断下一个是否为&
            if(c == '&'){
                builder.append((char) c);
                content = builder.toString();
                tokenType = TokenType.AND;
                token = new Token(content, tokenType, lineNum);
            }else{
                //错误处理
                errorRecorder.addError(ErrorType.ILLEGAL_SYMBOL, lineNum);
                return next();
            }
        } else if (c == '|') {
            c = getChar();
            if(c == '|'){
                builder.append((char) c);
                content = builder.toString();
                tokenType = TokenType.OR;
                token = new Token(content, tokenType, lineNum);
            }else {
                //错误处理
                errorRecorder.addError(ErrorType.ILLEGAL_SYMBOL, lineNum);
                return next();
            }
        } else if (c == '+') {
            content = builder.toString();
            tokenType = TokenType.PLUS;
            token = new Token(content, tokenType, lineNum);
        } else if (c == '-') {
            content = builder.toString();
            tokenType = TokenType.MINU;
            token = new Token(content, tokenType, lineNum);
        } else if (c == '*') {
            content = builder.toString();
            tokenType = TokenType.MULT;
            token = new Token(content, tokenType, lineNum);
        } else if (c == '/') { //除号，单行注释，多行注释
            c = getChar();
            if (c == '*') { // 多行注释
                builder.append((char) c);
                c = getChar();
                while (c != EOF){
                    while (c != '*'){
                        builder.append((char) c);
                        if(c == '\n'){
                            lineNum++; //在注释中换行
                        }
                        c = getChar();
                    }
                    while (c == '*'){ // *后面可能有多个*
                        builder.append((char) c);
                        c = getChar();
                    }
                    if(c == '/'){
                        builder.append((char) c);
                        return next();
                    }
                }
                backChar(c);
            } else if (c == '/') {
                builder.append((char) c);
                c = getChar();
                while (c != EOF && c != '\n'){
                    builder.append((char) c);
                    c = getChar();
                }
                backChar(c);
                return next();
            } else {
                backChar(c); //不读下一个字符
                content = builder.toString();
                tokenType = TokenType.DIV;
                token = new Token(content, tokenType, lineNum);
            }
        } else if (c == '%') {
            content = builder.toString();
            tokenType = TokenType.MOD;
            token = new Token(content, tokenType, lineNum);
        } else if (c == '<') { // <  <=
            c = getChar();
            if (c == '=') {
                builder.append((char) c);
                content = builder.toString();
                tokenType = TokenType.LEQ;
                token = new Token(content, tokenType, lineNum);
            }else {
                backChar(c);//回退
                content = builder.toString();
                tokenType = TokenType.LSS;
                token = new Token(content, tokenType, lineNum);
            }
        } else if (c == '>') {
            c = getChar();
            if (c == '=') {
                builder.append((char) c);
                content = builder.toString();
                tokenType = TokenType.GEQ;
                token = new Token(content, tokenType, lineNum);
            }else {
                backChar(c);
                content = builder.toString();
                tokenType = TokenType.GRE;
                token = new Token(content, tokenType, lineNum);
            }
        } else if (c == '=') {
            c = getChar();
            if (c == '=') {
                builder.append((char) c);
                content = builder.toString();
                tokenType = TokenType.EQL;
                token = new Token(content, tokenType, lineNum);
            }else {
                backChar(c);
                content = builder.toString();
                tokenType = TokenType.ASSIGN;
                token = new Token(content, tokenType, lineNum);
            }
        } else if (c == ';') {
            content = builder.toString();
            tokenType = TokenType.SEMICN;
            token = new Token(content, tokenType, lineNum);
        } else if (c == ',') {
            content = builder.toString();
            tokenType = TokenType.COMMA;
            token = new Token(content, tokenType, lineNum);
        } else if (c == '(') {
            content = builder.toString();
            tokenType = TokenType.LPARENT;
            token = new Token(content, tokenType, lineNum);
        }else if (c == ')') {
            content = builder.toString();
            tokenType = TokenType.RPARENT;
            token = new Token(content, tokenType, lineNum);
        }else if (c == '[') {
            content = builder.toString();
            tokenType = TokenType.LBRACK;
            token = new Token(content, tokenType, lineNum);
        }else if (c == ']') {
            content = builder.toString();
            tokenType = TokenType.RBRACK;
            token = new Token(content, tokenType, lineNum);
        }else if (c == '{') {
            content = builder.toString();
            tokenType = TokenType.LBRACE;
            token = new Token(content, tokenType, lineNum);
        }else if (c == '}') {
            content = builder.toString();
            tokenType = TokenType.RBRACE;
            token = new Token(content, tokenType, lineNum);
        } else if (c == '\n') {
            lineNum++;
            return next();
        } else if (isWhitespaceWithoutLine(c)) { //除换行符外的空白符
            return next();
        } else if (c == EOF) {
            //backChar(c);
            return false;
        }else {
            //错误处理
            throw new LexerException();
        }
        return true; 
    }

    /**
     * 获取输入流中的字符，需要回退则返回回退的字符
     * @return
     */
    private int getChar() throws IOException {
        //需要回退
        if (hasBackChar) {
            hasBackChar = false;
            return backChar;//回退的字符
        }else {
            int nextChar = reader.read();// 下一个字符
            return nextChar;
        }
    }

    /**
     * 回退字符
     * @param c
     * @throws IOException
     */
    private void backChar(int c) throws IOException {
        hasBackChar = true;
        backChar = c;
    }


    private static boolean isDigit(int c){
        return c >= '0' && c <= '9';
    }

    //开头不是0（验证数字）
    private static boolean isPositiveDigit(int c){
        return c >= '1' && c <= '9';
    }

    /**
     * 可以当做标识符的首字符
     * @param c
     * @return
     */
    private static boolean isLetter(int c){
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c == '_');
    }

    private static boolean isNormalLetter(int c){
        return c == 32 || c == 33 || (c >= 40 && c <= 126);
    }

    private static boolean isWhitespaceWithoutLine(int c){
        return Character.isWhitespace(c) && c != '\n';
    }

    public Token getToken(){
        if(token != null){
            return token;
        }
        return null;
    }
}
