package frontend.parser;

import error.CompileError;
import error.ErrorRecorder;
import error.ErrorType;
import exception.LexerException;
import exception.ParserException;
import frontend.lexer.Lexer;
import frontend.lexer.Token;
import frontend.lexer.TokenType;
import frontend.parser.node.CompUnit;
import frontend.parser.node.Node;
import frontend.parser.node.declaration.*;
import frontend.parser.node.expression.*;
import frontend.parser.node.function.*;
import frontend.parser.node.statement.*;
import frontend.parser.node.variable.ConstInitVal;
import frontend.parser.node.variable.InitVal;
import frontend.parser.node.variable.LVal;
import frontend.parser.node.variable.Number;

import java.io.IOException;

public class Parser {
    private PreReadBuffer buf;
    private ErrorRecorder errorRecorder;

    public Parser(Lexer lexer, ErrorRecorder errorRecorder) throws IOException, LexerException {
        this.buf = new PreReadBuffer(lexer, 3);
        this.errorRecorder = errorRecorder;
    }

    private static boolean isMatch(Token token, TokenType type) {
        return token.getTokenType() == type;
    }
    private static boolean isNotMatch(Token token, TokenType type) {
        return token.getTokenType() != type;
    }
    private static void matchOrThrow(Token token, TokenType type, ParserException e) throws ParserException {
        if (isNotMatch(token, type)) {
            throw e;
        }
    }
    private Token parseToken(Token token, TokenType type, ParserException onFail) throws LexerException, ParserException, IOException {
        matchOrThrow(token, type, onFail);
        return buf.readNextToken();
    }

    public Node parse() throws LexerException, ParserException, IOException {
        Token currToken = buf.readNextToken();
        ParseResult result = parseCompUnit(currToken);
        currToken = result.getNextToken();
        matchOrThrow(currToken, null, new ParserException());  // if not reach end

        return result.getSubtree();
    }

    //CompUnit解析器 CompUnit → {Decl} {FuncDef} MainFuncDef
    private ParseResult parseCompUnit(Token currToken) throws ParserException, IOException, LexerException {
        CompUnit compUnit = new CompUnit();
        ParseResult result;
        Token preRead, prePreRead;

        // Decl
        preRead = buf.readTokenByOffset(1);
        prePreRead = buf.readTokenByOffset(2);
        while (isMatch(currToken, TokenType.CONSTTK)
        || (isMatch(currToken, TokenType.INTTK)
                && isMatch(preRead, TokenType.IDENFR)
                && isNotMatch(prePreRead, TokenType.LPARENT))
        ){
            result = parseDecl(currToken);
            currToken = result.getNextToken();
            compUnit.decls.add((Decl) result.getSubtree());

            preRead = buf.readTokenByOffset(1);
            prePreRead = buf.readTokenByOffset(2);
        }

        // FuncDef
        preRead = buf.readTokenByOffset(1);
        while (isMatch(currToken, TokenType.VOIDTK)
        || (isMatch(currToken, TokenType.INTTK)
                && isMatch(preRead, TokenType.IDENFR))
        ){
            result = parseFuncDef(currToken);
            currToken = result.getNextToken();
            compUnit.funcDefs.add((FuncDef) result.getSubtree());

            preRead = buf.readTokenByOffset(1);
        }

        // MainFuncDef
        result = parseMainFuncDef(currToken);
        currToken = result.getNextToken();
        compUnit.mainFuncDef = (MainFuncDef) result.getSubtree();

        return new ParseResult(currToken, compUnit);
    }

    //Decl解析器 Decl → ConstDecl | VarDecl
    private ParseResult parseDecl(Token currToken) throws ParserException, IOException, LexerException {
        Decl decl;
        ParseResult result;

        if(isMatch(currToken, TokenType.CONSTTK)){
            // Decl → ConstDecl
            Decl declForConst = new Decl(1);

            result = parseConstDecl(currToken);
            currToken = result.getNextToken();
            declForConst.constDecl = (ConstDecl) result.getSubtree();

            decl = declForConst;
        } else if (isMatch(currToken, TokenType.INTTK)) {
            // Decl → VarDecl
            Decl declForVar = new Decl(2);

            result = parseVarDecl(currToken);
            currToken = result.getNextToken();
            declForVar.varDecl = (VarDecl) result.getSubtree();

            decl = declForVar;
        }else {
            throw new ParserException();
        }

        return new ParseResult(currToken, decl);
    }

    // ConstDecl解析器 ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
    private ParseResult parseConstDecl(Token currToken) throws ParserException, IOException, LexerException {
        ConstDecl constDecl = new ConstDecl();
        ParseResult result;

        currToken = parseToken(currToken, TokenType.CONSTTK, new ParserException());

        result = parseBType(currToken);
        currToken = result.getNextToken();
        constDecl.type = (BType) result.getSubtree();

        result = parseConstDef(currToken);
        currToken = result.getNextToken();
        constDecl.constDefs.add((ConstDef) result.getSubtree());

        while (isMatch(currToken, TokenType.COMMA)){
            currToken = buf.readNextToken();

            result = parseConstDef(currToken);
            currToken = result.getNextToken();
            constDecl.constDefs.add((ConstDef) result.getSubtree());
        }

        if(isMatch(currToken, TokenType.SEMICN)){
            currToken = buf.readNextToken();
        }else {
            // 缺少分号
            errorRecorder.addError(ErrorType.SEMICN_MISS, buf.readPreToken().getLineNum());
        }

        return new ParseResult(currToken, constDecl);
    }

    //ConstDef解析器 ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
    private ParseResult parseConstDef(Token currToken) throws ParserException, IOException, LexerException {
        ConstDef constDef = new ConstDef();
        ParseResult result;

        constDef.ident = currToken.getTokenContent();
        constDef.identLineNum = currToken.getLineNum();
        currToken = parseToken(currToken, TokenType.IDENFR, new ParserException());
        while (isMatch(currToken, TokenType.LBRACK)){
            currToken = buf.readNextToken();

            result = parseConstExp(currToken);
            currToken = result.getNextToken();
            constDef.dimensions.add((ConstExp) result.getSubtree());

            if(isMatch(currToken, TokenType.RBRACK)){
                currToken = buf.readNextToken();
            }else {
                errorRecorder.addError(ErrorType.RBRACK_MISS, buf.readPreToken().getLineNum());
            }
        }
        currToken = parseToken(currToken, TokenType.ASSIGN, new ParserException());

        result = parseConstInitVal(currToken);
        currToken = result.getNextToken();
        constDef.constInitVal = (ConstInitVal) result.getSubtree();

        return new ParseResult(currToken, constDef);
    }

    // ConstInitVal解析器 ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}'
    private ParseResult parseConstInitVal(Token currToken) throws ParserException, IOException, LexerException {
        ConstInitVal constInitVal;
        ParseResult result;

        if(isMatch(currToken, TokenType.LPARENT)
            || isMatch(currToken, TokenType.IDENFR)
            || isMatch(currToken, TokenType.INTCON)
            || isMatch(currToken, TokenType.PLUS)
            || isMatch(currToken, TokenType.MINU)
        ){
            ConstInitVal constInitValSingle = new ConstInitVal(1);

            result = parseConstExp(currToken);
            currToken = result.getNextToken();
            constInitValSingle.constExp = (ConstExp) result.getSubtree();

            constInitVal = constInitValSingle;
        } else if (isMatch(currToken, TokenType.LBRACE)) {
            ConstInitVal constInitValDouble = new ConstInitVal(2);

            currToken = buf.readNextToken();
            if(isNotMatch(currToken, TokenType.RBRACE)){
                result = parseConstExp(currToken);
                currToken = result.getNextToken();
                constInitValDouble.constExps.add((ConstExp) result.getSubtree());

                while (isMatch(currToken, TokenType.COMMA)){
                    currToken = buf.readNextToken();

                    result = parseConstExp(currToken);
                    currToken = result.getNextToken();
                    constInitValDouble.constExps.add((ConstExp) result.getSubtree());
                }
            }
            currToken = parseToken(currToken, TokenType.RBRACE,  new ParserException());

            constInitVal = constInitValDouble;
        }else {
            throw new ParserException();
        }

        return new ParseResult(currToken, constInitVal);
    }

    // VarDecl解析器 VarDecl → [ 'static' ] BType VarDef { ',' VarDef } ';'
    private ParseResult parseVarDecl(Token currToken) throws ParserException, IOException, LexerException {
        VarDecl varDecl = new VarDecl();
        ParseResult result;

        // static 可选
        if(isMatch(currToken, TokenType.STATICTK)){
            varDecl = new VarDecl(1);
            currToken = parseToken(currToken, TokenType.STATICTK, new ParserException());
        }else {
            varDecl = new VarDecl(2);
        }

        result = parseBType(currToken);
        currToken = result.getNextToken();
        varDecl.type = (BType) result.getSubtree();

        result = parseVarDef(currToken);
        currToken = result.getNextToken();
        varDecl.varDefs.add((VarDef) result.getSubtree());

        while (isMatch(currToken, TokenType.COMMA)){
            currToken = buf.readNextToken();

            result = parseVarDef(currToken);
            currToken = result.getNextToken();
            varDecl.varDefs.add((VarDef) result.getSubtree());
        }

        if(isMatch(currToken, TokenType.SEMICN)){
            currToken = buf.readNextToken();
        }else {
            errorRecorder.addError(ErrorType.SEMICN_MISS, buf.readPreToken().getLineNum());
        }

        return new ParseResult(currToken, varDecl);
    }

    // VarDef解析器 VarDef → Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '=' InitVal
    private ParseResult parseVarDef(Token currToken) throws ParserException, IOException, LexerException {
        VarDef varDef = new VarDef();
        ParseResult result;

        varDef.ident = currToken.getTokenContent();
        varDef.identLineNum = currToken.getLineNum();
        currToken = parseToken(currToken, TokenType.IDENFR, new ParserException());

        while (isMatch(currToken, TokenType.LBRACK)){
            currToken = buf.readNextToken();

            result = parseConstExp(currToken);
            currToken = result.getNextToken();
            varDef.dimensions.add((ConstExp) result.getSubtree());

            if(isMatch(currToken, TokenType.RBRACK)){
                currToken = buf.readNextToken();
            }else {
                errorRecorder.addError(ErrorType.RBRACK_MISS, buf.readPreToken().getLineNum());
            }
        }

        if (isMatch(currToken, TokenType.ASSIGN)){
            currToken = buf.readNextToken();

            result = parseInitVal(currToken);
            currToken = result.getNextToken();
            varDef.initVal = (InitVal) result.getSubtree();
        }

        return new ParseResult(currToken, varDef);
    }

    // InitVal 解析器 InitVal → Exp | '{' [ Exp { ',' Exp } ] '}'
    private ParseResult parseInitVal(Token currToken) throws ParserException, IOException, LexerException {
        InitVal initVal;
        ParseResult result;

        if(isMatch(currToken, TokenType.LPARENT)
            || isMatch(currToken, TokenType.IDENFR)
            || isMatch(currToken, TokenType.INTCON)
            || isMatch(currToken, TokenType.PLUS)
            || isMatch(currToken, TokenType.MINU)
        ){
            InitVal initValSingle = new InitVal(1);

            result = parseExp(currToken);
            currToken = result.getNextToken();
            initValSingle.exp = (Exp) result.getSubtree();

            initVal = initValSingle;
        } else if (isMatch(currToken, TokenType.LBRACE)) {
            InitVal initValDouble = new InitVal(2);
            currToken = buf.readNextToken();

            if(isNotMatch(currToken, TokenType.RBRACE)){
                result = parseExp(currToken);
                currToken = result.getNextToken();
                initValDouble.exps.add((Exp) result.getSubtree());

                while (isMatch(currToken, TokenType.COMMA)){
                    currToken = buf.readNextToken();

                    result = parseExp(currToken);
                    currToken = result.getNextToken();
                    initValDouble.exps.add((Exp) result.getSubtree());
                }
            }
            currToken = parseToken(currToken, TokenType.RBRACE,  new ParserException());
            initVal = initValDouble;
        }else {
            throw new ParserException();
        }

        return new ParseResult(currToken, initVal);
    }

    // Exp 解析器 Exp → AddExp
    private ParseResult parseExp(Token currToken) throws ParserException, IOException, LexerException {
        Exp exp = new Exp();
        ParseResult result;

        result = parseAddExp(currToken);
        currToken = result.getNextToken();
        exp.addExp = (AddExp) result.getSubtree();

        return new ParseResult(currToken, exp);
    }

    // AddExp 解析器 AddExp → MulExp | AddExp ('+' | '−') MulExp
    private ParseResult parseAddExp(Token currToken) throws ParserException, IOException, LexerException {
        AddExp addExp = null;
        ParseResult result;

        // 先解析第一个 MulExp （公共开头）
        result = parseMulExp(currToken);
        currToken = result.getNextToken();
        MulExp firstMulExp = (MulExp) result.getSubtree();

        while(isMatch(currToken, TokenType.PLUS)
            || isMatch(currToken, TokenType.MINU)
        ){
            TokenType op = currToken.getTokenType();
            currToken = buf.readNextToken();

            result = parseMulExp(currToken);
            currToken = result.getNextToken();
            MulExp nextMulExp = (MulExp) result.getSubtree();

            AddExp newAddExp = new AddExp(2);
            // 如果是第一次循环，左侧 AddExp 用之前的 MulExp 构造（UType=1）
            newAddExp.addExp = (addExp == null) ? new AddExp(1) {{ mulExp1 = firstMulExp; }} : addExp;
            newAddExp.op = op;
            newAddExp.mulExp2 = nextMulExp;

            // 更新 addExp 为当前新构造的节点，用于下一次循环的左侧
            addExp = newAddExp;
        }

        // 只有单个 MulExp
        if(addExp == null){
            addExp = new AddExp(1);
            addExp.mulExp1 = firstMulExp;
        }

        return new ParseResult(currToken, addExp);
    }

    // MulExp 解析器 MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    private ParseResult parseMulExp(Token currToken) throws ParserException, IOException, LexerException {
        MulExp mulExp = new MulExp(1);
        ParseResult result;

        // 解析第一个 UnaryExp（两种产生式的公共起始部分）
        result = parseUnaryExp(currToken);
        currToken = result.getNextToken();
        mulExp.unaryExp1 = (UnaryExp) result.getSubtree();

        while (isMatch(currToken, TokenType.MULT)
                || isMatch(currToken, TokenType.DIV)
                || isMatch(currToken, TokenType.MOD)) {
            // 记录当前乘除模运算符
            TokenType op = currToken.getTokenType();
            // 消耗运算符 Token，推进到下一个 Token
            currToken = buf.readNextToken();

            // 解析运算符右侧的 UnaryExp
            result = parseUnaryExp(currToken);
            currToken = result.getNextToken();
            UnaryExp nextUnaryExp = (UnaryExp) result.getSubtree();

            // 构造 UType=2 的 MulExp（对应“MulExp op UnaryExp”产生式）
            MulExp newMulExp = new MulExp(2);
            newMulExp.mulExp = mulExp;       // 左侧为之前已构建的完整 MulExp
            newMulExp.op = op;               // 当前匹配的运算符
            newMulExp.unaryExp2 = nextUnaryExp; // 右侧的 UnaryExp

            // 更新 mulExp 为新节点，用于下一轮循环的左侧表达式
            mulExp = newMulExp;
        }

        return new ParseResult(currToken, mulExp);
    }

    // UnaryExp 解析器 UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    private ParseResult parseUnaryExp(Token currToken) throws ParserException, IOException, LexerException {
        UnaryExp unaryExp;
        ParseResult result;

        Token preRead = buf.readTokenByOffset(1);

        if(isMatch(currToken, TokenType.LPARENT)
            || (isMatch(currToken, TokenType.IDENFR)
                && isNotMatch(preRead, TokenType.LPARENT))
            ||isMatch(currToken, TokenType.INTCON)
        ){
            UnaryExp unaryExpForPrimary = new UnaryExp(1);

            result = parsePrimaryExp(currToken);
            currToken = result.getNextToken();
            unaryExpForPrimary.primaryExp = (PrimaryExp) result.getSubtree();

            unaryExp = unaryExpForPrimary;
        } else if (isMatch(currToken, TokenType.IDENFR)
                && isMatch(preRead, TokenType.LPARENT)
        ) {
            UnaryExp unaryExpForFunc = new UnaryExp(2);
            unaryExpForFunc.ident = currToken.getTokenContent();
            unaryExpForFunc.identLineNum = currToken.getLineNum();

            currToken = parseToken(currToken, TokenType.IDENFR, new ParserException());
            currToken = parseToken(currToken, TokenType.LPARENT,  new ParserException());

            if(isMatch(currToken, TokenType.LPARENT)
                || isMatch(currToken, TokenType.IDENFR)
                || isMatch(currToken, TokenType.INTCON)
                || isMatch(currToken, TokenType.PLUS)
                || isMatch(currToken, TokenType.MINU)
            ){
                result = parseFuncRParams(currToken);
                currToken = result.getNextToken();
                unaryExpForFunc.funcRParams = (FuncRParams) result.getSubtree();
            }

            if(isMatch(currToken, TokenType.RPARENT)){
                currToken = buf.readNextToken();
            }else {
                errorRecorder.addError(ErrorType.RPARENT_MISS, buf.readPreToken().getLineNum());
            }

            unaryExp = unaryExpForFunc;
        } else if (isMatch(currToken, TokenType.PLUS)
            || isMatch(currToken, TokenType.MINU)
            || isMatch(currToken, TokenType.NOT)
        ) {
            UnaryExp unaryExpForUnaryOp = new UnaryExp(3);

            result = parseUnaryOp(currToken);
            currToken = result.getNextToken();
            unaryExpForUnaryOp.unaryOp = (UnaryOp) result.getSubtree();

            result = parseUnaryExp(currToken);
            currToken = result.getNextToken();
            unaryExpForUnaryOp.unaryExp = (UnaryExp) result.getSubtree();

            unaryExp = unaryExpForUnaryOp;
        }else {
            throw new ParserException();
        }

        return new ParseResult(currToken, unaryExp);
    }

    // UnaryOp 解析器 UnaryOp → '+' | '−' | '!'
    private ParseResult parseUnaryOp(Token currToken) throws ParserException, IOException, LexerException {
        UnaryOp unaryOp = new UnaryOp();

        if(isMatch(currToken, TokenType.PLUS)
            || isMatch(currToken, TokenType.MINU)
            || isMatch(currToken, TokenType.NOT)
        ){
            unaryOp.op = currToken.getTokenType();
            currToken = buf.readNextToken();
        }else {
            throw new ParserException();
        }

        return new ParseResult(currToken, unaryOp);
    }

    // PrimaryExp 解析器 PrimaryExp → '(' Exp ')' | LVal | Number
    private ParseResult parsePrimaryExp(Token currToken) throws ParserException, IOException, LexerException {
        PrimaryExp primaryExp;
        ParseResult result;

        if (isMatch(currToken, TokenType.LPARENT)) {
            // PrimaryExp → '(' Exp ')'
            primaryExp = new PrimaryExp(1);

            currToken = buf.readNextToken(); // 消耗左括号

            result = parseExp(currToken);
            currToken = result.getNextToken();
            primaryExp.exp = (Exp) result.getSubtree();

            if (isMatch(currToken, TokenType.RPARENT)) {
                currToken = buf.readNextToken();
            }else {
                errorRecorder.addError(ErrorType.RPARENT_MISS, buf.readPreToken().getLineNum());
            }
        } else if (isMatch(currToken, TokenType.IDENFR)) {
            // PrimaryExp → LVal
            primaryExp = new PrimaryExp(2);

            result = parseLVal(currToken);
            currToken = result.getNextToken();
            primaryExp.lVal = (LVal) result.getSubtree();
        } else if (isMatch(currToken, TokenType.INTCON)) {
            // PrimaryExp → Number
            primaryExp = new PrimaryExp(3);

            result = parseNumber(currToken);
            currToken = result.getNextToken();
            primaryExp.number = (Number) result.getSubtree();
        }else {
            throw new ParserException();
        }
        return new ParseResult(currToken, primaryExp);
    }

    // LVal 解析器 LVal → Ident ['[' Exp ']']
    private ParseResult parseLVal(Token currToken) throws ParserException, IOException, LexerException {
        LVal lVal = new LVal();
        ParseResult result;

        lVal.ident = currToken.getTokenContent();
        lVal.identLineNum = currToken.getLineNum();
        currToken = parseToken(currToken, TokenType.IDENFR, new ParserException());

        while (isMatch(currToken, TokenType.LBRACK)){
            currToken = buf.readNextToken();

            result = parseExp(currToken);
            currToken = result.getNextToken();
            lVal.dimensions.add((Exp) result.getSubtree());

            if (isMatch(currToken, TokenType.RBRACK)) {
                currToken = buf.readNextToken();
            }else {
                errorRecorder.addError(ErrorType.RBRACK_MISS, buf.readPreToken().getLineNum());
            }
        }

        return new ParseResult(currToken, lVal);
    }

    // Number 解析器 Number → IntConst
    private ParseResult parseNumber(Token currToken) throws ParserException, IOException, LexerException {
        Number number = new Number();
        number.intConst = currToken.getTokenContent();

        currToken = parseToken(currToken, TokenType.INTCON, new ParserException());

        return new ParseResult(currToken, number);
    }

    // ConstExp 解析器 ConstExp → AddExp
    private ParseResult parseConstExp(Token currToken) throws ParserException, IOException, LexerException {
        ConstExp constExp = new ConstExp();
        ParseResult result;

        result = parseAddExp(currToken);
        currToken = result.getNextToken();
        constExp.addExp = (AddExp) result.getSubtree();

        return new ParseResult(currToken, constExp);
    }

    // BType 解析器 BType → 'int'
    private ParseResult parseBType(Token currToken) throws ParserException, IOException, LexerException {
        BType bType = new BType();

        bType.type = currToken.getTokenType();
        currToken = parseToken(currToken, TokenType.INTTK, new ParserException());

        return new ParseResult(currToken, bType);
    }

    // FuncDef 解析器 FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
    private ParseResult parseFuncDef(Token currToken) throws ParserException, IOException, LexerException {
        FuncDef funcDef = new FuncDef();
        ParseResult result;

        result = parseFuncType(currToken);
        currToken = result.getNextToken();
        funcDef.funcType = (FuncType) result.getSubtree();

        funcDef.ident = currToken.getTokenContent();
        funcDef.identLineNum = currToken.getLineNum();
        currToken = parseToken(currToken, TokenType.IDENFR, new ParserException());
        currToken = parseToken(currToken, TokenType.LPARENT, new ParserException());

        if (isMatch(currToken, TokenType.INTTK)){
            result = parseFuncFParams(currToken);
            currToken = result.getNextToken();
            funcDef.funcFParams = (FuncFParams) result.getSubtree();
        }

        if (isMatch(currToken, TokenType.RPARENT)) {
            currToken = buf.readNextToken();
        }else {
            errorRecorder.addError(ErrorType.RPARENT_MISS, buf.readPreToken().getLineNum());
        }

        result = parseBlock(currToken);
        funcDef.block = (Block) result.getSubtree();

        currToken = result.getNextToken();

        return new ParseResult(currToken, funcDef);
    }

    // FuncType 解析器 FuncType → 'void' | 'int'
    private ParseResult parseFuncType(Token currToken) throws ParserException, IOException, LexerException {
        FuncType funcType = new FuncType();

        if(isMatch(currToken, TokenType.VOIDTK) || isMatch(currToken, TokenType.INTTK)){
            funcType.type = currToken.getTokenType();
            currToken = buf.readNextToken();
        }else {
            throw new ParserException();
        }

        return new ParseResult(currToken, funcType);
    }

    // parseFuncFParams 解析器 FuncFParams → FuncFParam { ',' FuncFParam }
    private ParseResult parseFuncFParams(Token currToken) throws ParserException, IOException, LexerException {
        FuncFParams funcFParams = new FuncFParams();
        ParseResult result;

        result = parseFuncFParam(currToken);
        currToken = result.getNextToken();
        funcFParams.funcFParams.add((FuncFParam) result.getSubtree());

        while (isMatch(currToken, TokenType.COMMA)){
            currToken = buf.readNextToken();
            result = parseFuncFParam(currToken);
            funcFParams.funcFParams.add((FuncFParam) result.getSubtree());

            currToken = result.getNextToken();
        }

        return new ParseResult(currToken, funcFParams);
    }

    // FuncFParam 解析器 FuncFParam → BType Ident ['[' ']']
    private ParseResult parseFuncFParam(Token currToken) throws ParserException, IOException, LexerException {
        FuncFParam funcFParam = new FuncFParam();
        ParseResult result;

        result = parseBType(currToken);
        currToken = result.getNextToken();
        funcFParam.type = (BType) result.getSubtree();

        funcFParam.ident = currToken.getTokenContent();
        funcFParam.identLineNum = currToken.getLineNum();
        currToken = parseToken(currToken, TokenType.IDENFR, new ParserException());

        while (isMatch(currToken, TokenType.LBRACK)) {
            currToken = buf.readNextToken(); // 消耗[

            if (isMatch(currToken, TokenType.RBRACK)) {
                currToken = buf.readNextToken();
                funcFParam.count++; // 记录一对[]
            }else {
                errorRecorder.addError(ErrorType.RBRACK_MISS, buf.readPreToken().getLineNum());
            }
        }

        return new ParseResult(currToken, funcFParam);
    }

    // FuncRParams 解析器 FuncRParams → Exp { ',' Exp }
    private ParseResult parseFuncRParams(Token currToken) throws ParserException, IOException, LexerException {
        FuncRParams funcRParams = new FuncRParams();
        ParseResult result;

        result = parseExp(currToken);
        currToken = result.getNextToken();
        funcRParams.exps.add((Exp) result.getSubtree());

        while (isMatch(currToken, TokenType.COMMA)) {
            currToken = buf.readNextToken();

            result = parseExp(currToken);
            currToken = result.getNextToken();
            funcRParams.exps.add((Exp) result.getSubtree());
        }

        return new ParseResult(currToken, funcRParams);
    }

    // Block 解析器 Block → '{' { BlockItem } '}'
    private ParseResult parseBlock(Token currToken) throws ParserException, IOException, LexerException {
        Block block = new Block();
        ParseResult result;

        currToken = parseToken(currToken, TokenType.LBRACE, new ParserException());
        while (isNotMatch(currToken, TokenType.RBRACE)) {
            result = parseBlockItem(currToken);
            currToken = result.getNextToken();
            block.blockItems.add((BlockItem) result.getSubtree());
        }
        block.blockRLineNum = currToken.getLineNum();
        currToken = buf.readNextToken();

        return new ParseResult(currToken, block);
    }

    // BlockItem 解析器 BlockItem → Decl | Stmt
    private ParseResult parseBlockItem(Token currToken) throws ParserException, IOException, LexerException {
        BlockItem blockItem;
        ParseResult result;

        if(isMatch(currToken, TokenType.INTTK) || isMatch(currToken, TokenType.CONSTTK)){
            // BlockItem → Decl
            blockItem = new BlockItem(1);

            result = parseDecl(currToken);
            currToken = result.getNextToken();
            blockItem.decl = (Decl) result.getSubtree();
        }else {
            // BlockItem → Stmt
            blockItem = new BlockItem(2);

            result = parseStmt(currToken);
            currToken = result.getNextToken();
            blockItem.stmt = (Stmt) result.getSubtree();
        }

        return new ParseResult(currToken, blockItem);
    }

    // Stmt 解析器
    /**
     * Stmt → LVal '=' Exp ';'
     * | [Exp] ';'
     * | Block
     * | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
     * | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
     * | 'break' ';'
     * | 'continue' ';'
     * | 'return' [Exp] ';'
     */
    private ParseResult parseStmt(Token currToken) throws ParserException, IOException, LexerException {
        Stmt stmt;
        ParseResult result;

        Token preRead = buf.readTokenByOffset(1);

        // Stmt → LVal '=' Exp ';'
        if(isMatch(currToken, TokenType.IDENFR)
                && (buf.findUntil(TokenType.ASSIGN, TokenType.SEMICN))
        ){
            LVal tmpLVal;
            result = parseLVal(currToken);
            currToken = result.getNextToken();
            tmpLVal = (LVal) result.getSubtree();

            currToken = parseToken(currToken, TokenType.ASSIGN, new ParserException());
            if(isMatch(currToken, TokenType.LPARENT)
                || isMatch(currToken, TokenType.IDENFR)
                || isMatch(currToken, TokenType.INTCON)
                || isMatch(currToken, TokenType.PLUS)
                || isMatch(currToken, TokenType.MINU)
            ){
                stmt = new Stmt(1);
                
                result = parseExp(currToken);
                currToken = result.getNextToken();
                stmt.exp1 = (Exp) result.getSubtree();
                stmt.lVal = tmpLVal;
                
                if (isMatch(currToken, TokenType.SEMICN)){
                    currToken = buf.readNextToken();
                }else{
                    errorRecorder.addError(ErrorType.SEMICN_MISS, buf.readPreToken().getLineNum());
                }
            }else {
                throw new ParserException();
            }
        } else if (isMatch(currToken, TokenType.LPARENT)
            || isMatch(currToken, TokenType.IDENFR)
            || isMatch(currToken, TokenType.INTCON)
            || isMatch(currToken, TokenType.PLUS)
            || isMatch(currToken, TokenType.MINU)
        ){
            // Stmt → [Exp] ';' Exp存在
            stmt = new Stmt(2);
            
            result = parseExp(currToken);
            currToken = result.getNextToken();
            stmt.exp2 = (Exp) result.getSubtree();
            
            if(isMatch(currToken, TokenType.SEMICN)){
                currToken = buf.readNextToken();
            }else {
                errorRecorder.addError(ErrorType.SEMICN_MISS, buf.readPreToken().getLineNum());
            }
        } else if (isMatch(currToken, TokenType.SEMICN)) {
            // Stmt → [Exp] ';' Exp不存在
            currToken = buf.readNextToken();
            stmt = new Stmt(2);
        } else if (isMatch(currToken, TokenType.LBRACE)) {
            // Stmt → Block
            stmt = new Stmt(3);
            
            result = parseBlock(currToken);
            currToken = result.getNextToken();
            stmt.block = (Block) result.getSubtree();
        } else if (isMatch(currToken, TokenType.IFTK)) {
            // Stmt -> 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
            stmt = new Stmt(4);

            currToken = buf.readNextToken();
            currToken = parseToken(currToken, TokenType.LPARENT, new ParserException());

            result = parseCond(currToken);
            currToken = result.getNextToken();
            stmt.cond1 = (Cond) result.getSubtree();

            if (isMatch(currToken, TokenType.RPARENT)){
                currToken = buf.readNextToken();
            }else {
                errorRecorder.addError(ErrorType.RPARENT_MISS, buf.readPreToken().getLineNum());
            }

            result = parseStmt(currToken);
            currToken = result.getNextToken();
            stmt.ifStmt = (Stmt) result.getSubtree();

            if (isMatch(currToken, TokenType.ELSETK)){
                currToken = buf.readNextToken();

                result = parseStmt(currToken);
                currToken = result.getNextToken();
                stmt.elseStmt = (Stmt) result.getSubtree();
            }
        } else if (isMatch(currToken, TokenType.FORTK)) {
            // Stmt -> 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
            stmt = new Stmt(5);

            currToken = buf.readNextToken();
            currToken = parseToken(currToken, TokenType.LPARENT, new ParserException());
            if (isMatch(currToken, TokenType.IDENFR)){
                result = parseForStmt(currToken);
                currToken = buf.readNextToken();
                stmt.forStmt1 = (ForStmt) result.getSubtree();
            }

            if (isMatch(currToken, TokenType.SEMICN)){
                currToken = buf.readNextToken();
            }else {
                errorRecorder.addError(ErrorType.SEMICN_MISS, buf.readPreToken().getLineNum());
            }

            if (isMatch(currToken, TokenType.LPARENT)
                || isMatch(currToken, TokenType.IDENFR)
                || isMatch(currToken, TokenType.INTCON)
                || isMatch(currToken, TokenType.PLUS)
                || isMatch(currToken, TokenType.MINU)
                || isMatch(currToken, TokenType.NOT)
            ){
                result = parseCond(currToken);
                currToken = result.getNextToken();
                stmt.cond2 = (Cond) result.getSubtree();
            }

            if (isMatch(currToken, TokenType.SEMICN)){
                currToken = buf.readNextToken();
            }else {
                errorRecorder.addError(ErrorType.SEMICN_MISS, buf.readPreToken().getLineNum());
            }

            if (isMatch(currToken, TokenType.IDENFR)){
                result = parseForStmt(currToken);
                currToken = buf.readNextToken();
                stmt.forStmt2 = (ForStmt) result.getSubtree();
            }

            if (isMatch(currToken, TokenType.RPARENT)){
                currToken = buf.readNextToken();
            }else {
                errorRecorder.addError(ErrorType.RPARENT_MISS, buf.readPreToken().getLineNum());
            }

            result = parseStmt(currToken);
            currToken = result.getNextToken();
            stmt.stmt = (Stmt) result.getSubtree();
        } else if (isMatch(currToken, TokenType.BREAKTK)) {
            stmt = new Stmt(6);
            
            stmt.tkLineNum = currToken.getLineNum();
            stmt.type = currToken.getTokenType();
            
            currToken = buf.readNextToken();
            
            if (isMatch(currToken, TokenType.SEMICN)){
                currToken = buf.readNextToken();
            }else {
                errorRecorder.addError(ErrorType.SEMICN_MISS, buf.readPreToken().getLineNum());
            }
        } else if (isMatch(currToken, TokenType.CONTINUETK)) {
            stmt = new Stmt(6);

            stmt.tkLineNum = currToken.getLineNum();
            stmt.type = currToken.getTokenType();

            currToken = buf.readNextToken();

            if (isMatch(currToken, TokenType.SEMICN)){
                currToken = buf.readNextToken();
            }else {
                errorRecorder.addError(ErrorType.SEMICN_MISS, buf.readPreToken().getLineNum());
            }
        } else if (isMatch(currToken, TokenType.RETURNTK)) {
            // Stmt -> 'return' [Exp] ';'
            stmt = new Stmt(7);
            stmt.returnLineNum = currToken.getLineNum();
            
            currToken = buf.readNextToken();
            if (isMatch(currToken, TokenType.LPARENT)
                || isMatch(currToken, TokenType.IDENFR)
                    || isMatch(currToken, TokenType.INTCON)
                    || isMatch(currToken, TokenType.PLUS)
                    || isMatch(currToken, TokenType.MINU)
            ){
              result = parseExp(currToken);
              currToken = result.getNextToken();
              stmt.exp3 = (Exp) result.getSubtree();
            }
            
            if (isMatch(currToken, TokenType.SEMICN)){
                currToken = buf.readNextToken();
            }else {
                errorRecorder.addError(ErrorType.SEMICN_MISS, buf.readPreToken().getLineNum());
            }
        } else if (isMatch(currToken, TokenType.PRINTFTK)) {
            // Stmt -> 'printf''('StringConst {','Exp}')'';'
            stmt = new Stmt(8);
            stmt.printfLineNum = currToken.getLineNum();

            currToken = buf.readNextToken();
            currToken = parseToken(currToken, TokenType.LPARENT, new ParserException());

            stmt.stringConst = currToken.getTokenContent();
            currToken = parseToken(currToken, TokenType.STRCON, new ParserException());

            while (isMatch(currToken, TokenType.COMMA)){
                currToken = buf.readNextToken();

                result = parseExp(currToken);
                currToken = result.getNextToken();
                stmt.exps.add((Exp) result.getSubtree());
            }

            if (isMatch(currToken, TokenType.RPARENT)){
                currToken = buf.readNextToken();
            }else {
                errorRecorder.addError(ErrorType.RPARENT_MISS, buf.readPreToken().getLineNum());
            }

            if (isMatch(currToken, TokenType.SEMICN)){
                currToken = buf.readNextToken();
            }else {
                errorRecorder.addError(ErrorType.SEMICN_MISS, buf.readPreToken().getLineNum());
            }
        }else {
            throw new ParserException();
        }

        return new ParseResult(currToken, stmt);
    }

    // Cond 解析器 Cond → LOrExp
    private ParseResult parseCond(Token currToken) throws ParserException, LexerException, IOException {
        Cond cond = new Cond();
        ParseResult result;

        result = parseLOrExp(currToken);
        currToken = result.getNextToken();
        cond.lOrExp = (LOrExp) result.getSubtree();

        return new ParseResult(currToken, cond);
    }

    // ForStmt 解析器 ForStmt → LVal '=' Exp { ',' LVal '=' Exp }
    private ParseResult parseForStmt(Token currToken) throws ParserException, LexerException, IOException {
        ForStmt forStmt = new ForStmt();
        ParseResult result;

        // 解析第一个
        result = parseLVal(currToken);
        currToken = result.getNextToken();
        forStmt.lVals.add((LVal) result.getSubtree());

        currToken = parseToken(currToken, TokenType.ASSIGN, new ParserException());

        result = parseExp(currToken);
        currToken = result.getNextToken();
        forStmt.exps.add((Exp) result.getSubtree());

        while (isMatch(currToken, TokenType.COMMA)){
            currToken = parseToken(currToken, TokenType.COMMA, new ParserException());

            result = parseLVal(currToken);
            currToken = result.getNextToken();
            forStmt.lVals.add((LVal) result.getSubtree());

            currToken = parseToken(currToken, TokenType.ASSIGN, new ParserException());

            result = parseExp(currToken);
            currToken = result.getNextToken();
            forStmt.exps.add((Exp) result.getSubtree());
        }

        return new ParseResult(currToken, forStmt);
    }

    // LOrExp 解析器 LOrExp → LAndExp | LOrExp '||' LAndExp
    private ParseResult parseLOrExp(Token currToken) throws ParserException, LexerException, IOException {
        ParseResult result;

        result = parseLAndExp(currToken);
        currToken = result.getNextToken();
        LOrExp lOrExp = new LOrExp(1);
        lOrExp.lAndExp1 = (LAndExp) result.getSubtree();

        while (isMatch(currToken, TokenType.OR)){
            currToken = buf.readNextToken();

            result = parseLAndExp(currToken);
            currToken = result.getNextToken();
            LAndExp nextLAndExp = (LAndExp) result.getSubtree();

            // 创建新的双分支结点，将当前结点作为左子树
            LOrExp newLOrExp = new LOrExp(2);
            newLOrExp.lOrExp = lOrExp;
            newLOrExp.lAndExp2 = nextLAndExp;

            lOrExp = newLOrExp;
        }

        return new ParseResult(currToken, lOrExp);
    }

    // LAndExp 解析器 LAndExp → EqExp | LAndExp '&&' EqExp
    private ParseResult parseLAndExp(Token currToken) throws ParserException, LexerException, IOException {
        ParseResult result;

        result = parseEqExp(currToken);
        currToken = result.getNextToken();
        LAndExp lAndExp = new LAndExp(1);
        lAndExp.eqExp1 = (EqExp) result.getSubtree();

        while (isMatch(currToken, TokenType.AND)){
            currToken = buf.readNextToken();

            result = parseEqExp(currToken);
            currToken = result.getNextToken();
            EqExp nextEqExp = (EqExp) result.getSubtree();

            LAndExp newLAndExp = new LAndExp(2);
            newLAndExp.lAndExp = lAndExp;
            newLAndExp.eqExp2 = nextEqExp;

            lAndExp = newLAndExp;
        }

        return new ParseResult(currToken, lAndExp);
    }

    // EqExp 解析器 EqExp → RelExp | EqExp ('==' | '!=') RelExp
    private ParseResult parseEqExp(Token currToken) throws ParserException, LexerException, IOException {
        ParseResult result;

        result = parseRelExp(currToken);
        currToken = result.getNextToken();
        EqExp eqExp = new EqExp(1);
        eqExp.relExp1 = (RelExp) result.getSubtree();

        while (isMatch(currToken, TokenType.EQL)
                || isMatch(currToken, TokenType.NEQ)
        ){
            TokenType op = currToken.getTokenType();
            currToken = buf.readNextToken();

            result = parseRelExp(currToken);
            currToken = result.getNextToken();
            RelExp nextRelExp = (RelExp) result.getSubtree();

            EqExp newEqExp = new EqExp(2);
            newEqExp.eqExp = eqExp;
            newEqExp.op = op;
            newEqExp.relExp2 = nextRelExp;

            eqExp = newEqExp;
        }

        return new ParseResult(currToken, eqExp);
    }

    // RelExp 解析器 RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    private ParseResult parseRelExp(Token currToken) throws ParserException, LexerException, IOException {
        ParseResult result;

        result = parseAddExp(currToken);
        currToken = result.getNextToken();
        RelExp relExp = new RelExp(1);
        relExp.addExp1 = (AddExp) result.getSubtree();

        while (isMatch(currToken, TokenType.LSS)
            || isMatch(currToken, TokenType.GRE)
            || isMatch(currToken, TokenType.LEQ)
            || isMatch(currToken, TokenType.GEQ)
        ){
            TokenType op = currToken.getTokenType();
            currToken = buf.readNextToken();

            result = parseAddExp(currToken);
            currToken = result.getNextToken();
            AddExp nextAddExp = (AddExp) result.getSubtree();

            RelExp newRelExp = new RelExp(2);
            newRelExp.relExp = relExp;
            newRelExp.op = op;
            newRelExp.addExp2 = nextAddExp;

            relExp = newRelExp;
        }

        return new ParseResult(currToken, relExp);
    }

    // MainFuncDef 解析器 MainFuncDef → 'int' 'main' '(' ')' Block
    private ParseResult parseMainFuncDef(Token currToken) throws ParserException, LexerException, IOException {
        MainFuncDef mainFuncDef = new MainFuncDef();
        ParseResult result;

        currToken = parseToken(currToken, TokenType.INTTK, new ParserException());
        currToken = parseToken(currToken, TokenType.MAINTK, new ParserException());
        currToken = parseToken(currToken, TokenType.LPARENT, new ParserException());

        if (isMatch(currToken, TokenType.RPARENT)){
            currToken = buf.readNextToken();
        }else {
            errorRecorder.addError(ErrorType.RPARENT_MISS, buf.readPreToken().getLineNum());
        }

        result = parseBlock(currToken);
        currToken = result.getNextToken();
        mainFuncDef.block = (Block) result.getSubtree();

        return new ParseResult(currToken, mainFuncDef);
    }
}
