package frontend.visitor;

import backend.ir.Value;
import backend.ir.inst.BrInst;
import error.ErrorRecorder;
import error.ErrorType;
import frontend.lexer.TokenType;
import frontend.parser.node.CompUnit;
import frontend.parser.node.declaration.*;
import frontend.parser.node.expression.*;
import frontend.parser.node.function.*;
import frontend.parser.node.statement.*;
import frontend.parser.node.variable.ConstInitVal;
import frontend.parser.node.variable.InitVal;
import frontend.parser.node.variable.LVal;
import frontend.parser.node.variable.Number;
import frontend.symtable.SymbolTable;
import frontend.symtable.symbol.FunctionSymbol;
import frontend.symtable.symbol.Symbol;
import frontend.symtable.symbol.Type;
import frontend.symtable.symbol.VarSymbol;

import java.util.*;

public class Visitor {
    private final ErrorRecorder errorRecorder;
    // 全局根符号表
    private final SymbolTable symbolTable = new SymbolTable();
    private SymbolTable currentSymbolTable = symbolTable;
    private int isInLoop = 0;
    private boolean isRetExpNotNeed = false;

    private final Stack<List<BrInst>> continueBrInsts = new Stack<>();
    private final Stack<List<BrInst>> breakBrInsts = new Stack<>();

    private static final Set<String> LIBRARY_FUNCTIONS = new HashSet<>(Arrays.asList("getint", "printf"));

    public Visitor(ErrorRecorder errorRecorder) {
        this.errorRecorder = errorRecorder;
        initLibraryFunctions();
    }

    private void initLibraryFunctions() {
        for (String funcName : LIBRARY_FUNCTIONS) {
            FunctionSymbol funcSymbol = new FunctionSymbol();
            funcSymbol.ident = funcName;
            funcSymbol.retType.type = "int";

            symbolTable.insertSymbol(funcSymbol);
        }
    }

    // 编译单元 CompUnit → {Decl} {FuncDef} MainFuncDef
    public SymbolTable visitCompUnit(CompUnit compUnit) {
        for (Decl decl : compUnit.decls){
            visitDecl(decl);
        }

        for(FuncDef funcDef : compUnit.funcDefs){
            visitFuncDef(funcDef);
        }

        visitMainFuncDef(compUnit.mainFuncDef);

        return symbolTable;
    }

    // 声明 Decl → ConstDecl | VarDecl
    public void visitDecl(Decl decl) {
        if (decl.getUType() == 1){
            visitConstDecl(decl.constDecl);
        } else if (decl.getUType() == 2) {
            visitVarDecl(decl.varDecl);
        }
    }

    // 常量声明 ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
    public void visitConstDecl(ConstDecl constDecl) {
        for (ConstDef constDef : constDecl.constDefs){
            visitConstDef(constDef);
        }
    }

    // 常量定义 ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
    public void visitConstDef(ConstDef constDef) {
        if(currentSymbolTable.contains(constDef.ident)){
            errorRecorder.addError(ErrorType.NAME_REDEFINE, constDef.identLineNum);
            return;
        }

        // 常量符号创建
        VarSymbol varSymbol = new VarSymbol();
        varSymbol.ident = constDef.ident;
        varSymbol.isConst = true;
        varSymbol.varType.type = "int";

        // 若为数组常量
        for (ConstExp dimension : constDef.dimensions){
            varSymbol.varType.dims.add(visitConstExp(dimension).constVal);
        }

        // 常量初始化提取
        var r = visitConstInitVal(constDef.constInitVal);
        varSymbol.values.addAll(r.constInitVals);
        //var initValues = r.irValues;

        currentSymbolTable.insertSymbol(varSymbol);
    }

    // 常量初值 ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}'
    public VisitResult visitConstInitVal(ConstInitVal constInitVal){
        if(constInitVal.getUType() == 1){
            //ConstInitVal → ConstExp
            VisitResult visitResult = new VisitResult();
            var r = visitConstExp(constInitVal.constExp);
            visitResult.constInitVals.add(r.constVal);
            //visitResult.irValues.add(r.irValue);
            return visitResult;
        } else if (constInitVal.getUType() == 2) {
            //ConstInitVal → '{' [ ConstExp { ',' ConstExp } ] '}'
            VisitResult visitResult = new VisitResult();
            for (ConstExp constExp : constInitVal.constExps){
                var r = visitConstExp(constExp);
                visitResult.constInitVals.add(r.constVal);
                //visitResult.irValues.add(r.irValue);
            }
            return visitResult;
        }else {
            return new VisitResult();
        }
    }

    // 变量声明 VarDecl → [ 'static' ] BType VarDef { ',' VarDef } ';'
    public void visitVarDecl(VarDecl varDecl){
        visitBType(varDecl.type);

        boolean isStatic = varDecl.getUType() == 1;

        for (VarDef varDef : varDecl.varDefs){
            visitVarDef(varDef, isStatic);
        }
    }

    // 变量定义 VarDef → Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '=' InitVal
    public void visitVarDef(VarDef varDef, boolean isStatic){
        if (currentSymbolTable.contains(varDef.ident)){
            errorRecorder.addError(ErrorType.NAME_REDEFINE, varDef.identLineNum);
            return;
        }

        VarSymbol varSymbol = new VarSymbol();
        varSymbol.isConst = false;
        varSymbol.isStatic = isStatic;
        varSymbol.ident = varDef.ident;
        varSymbol.varType.type = "int";

        for (ConstExp dimension : varDef.dimensions){
            varSymbol.varType.dims.add(visitConstExp(dimension).constVal);
        }

        if (varDef.initVal != null){
            var r = visitInitVal(varDef.initVal);
            varSymbol.values.addAll(r.constInitVals);
        }

        currentSymbolTable.insertSymbol(varSymbol);
    }

    // 变量初值 InitVal → Exp | '{' [ Exp { ',' Exp } ] '}'
    public VisitResult visitInitVal(InitVal initVal){
        if (initVal.getUType() == 1){
            // InitVal → Exp
            VisitResult visitResult = new VisitResult();
            var r = visitExp(initVal.exp);
            visitResult.constInitVals.add(r.constVal);
            //visitResult.irValues.add(r.irValue);
            return visitResult;
        } else if (initVal.getUType() == 2) {
            // InitVal → '{' [ Exp { ',' Exp } ] '}'
            VisitResult visitResult = new VisitResult();
            for (Exp exp : initVal.exps){
                var r = visitExp(exp);
                visitResult.constInitVals.add(r.constVal);
                //visitResult.irValues.add(r.irValue);
            }
            return visitResult;
        }else {
            return new VisitResult();
        }
    }

    // 表达式 Exp → AddExp
    public VisitResult visitExp(Exp exp){
        return visitAddExp(exp.addExp);
    }

    // 加减表达式 AddExp → MulExp | AddExp ('+' | '−') MulExp
    public VisitResult visitAddExp(AddExp addExp){
        if (addExp.getUType() == 1){
            return visitMulExp(addExp.mulExp1);
        } else if (addExp.getUType() == 2) {
            VisitResult visitResult = new VisitResult();
            VisitResult r1= visitAddExp(addExp.addExp);
            var r2 = visitMulExp(addExp.mulExp2);
            Integer val1 = r1.constVal;
            Integer val2 = r2.constVal;

            assert r1.expType.equals(r2.expType);
            visitResult.expType = r1.expType;

            if (val1 != null && val2 != null){
                if(addExp.op == TokenType.PLUS){
                    visitResult.constVal = val1 + val2;
                }else {
                    visitResult.constVal = val1 - val2;
                }
            }
            return visitResult;
        }else {
            return new VisitResult();
        }
    }

    // 乘除模表达式 MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    public VisitResult visitMulExp(MulExp mulExp){
        if(mulExp.getUType() == 1){
            return visitUnaryExp(mulExp.unaryExp1);
        }else if(mulExp.getUType() == 2){
            VisitResult visitResult = new VisitResult();
            VisitResult r1 = visitMulExp(mulExp.mulExp);
            VisitResult r2 = visitUnaryExp(mulExp.unaryExp2);
            Integer val1 = r1.constVal;
            Integer val2 = r2.constVal;

            assert r1.expType.equals(r2.expType);
            visitResult.expType = r1.expType;

            if (val1 != null && val2 != null){
                if(mulExp.op == TokenType.MULT){
                    visitResult.constVal = val1 * val2;
                } else if (mulExp.op == TokenType.DIV) {
                    visitResult.constVal = val1 / val2;
                }else if (mulExp.op == TokenType.MOD) {
                    visitResult.constVal = val1 % val2;
                }
            }

            return visitResult;
        }else {
            return new VisitResult();
        }
    }

    // 一元表达式 UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    public VisitResult visitUnaryExp(UnaryExp unaryExp){
        if (unaryExp.getUType() == 1){
            return visitPrimaryExp(unaryExp.primaryExp);
        } else if (unaryExp.getUType() == 2) {
            // UnaryExp → Ident '(' [FuncRParams] ')'
            VisitResult visitResult = new VisitResult();
            Symbol symbol = currentSymbolTable.getSymbol(unaryExp.ident);

            if (symbol == null){
                errorRecorder.addError(ErrorType.UNDEFINED_NAME, unaryExp.identLineNum);
                visitResult.expType.type = "int";
                return visitResult;
            }

            FunctionSymbol functionSymbol = (FunctionSymbol) symbol;

            visitResult.expType = functionSymbol.retType;

            if (unaryExp.funcRParams != null){
                VisitResult r = visitFuncRParams(unaryExp.funcRParams);

                // 校验参数个数
                if(unaryExp.funcRParams.exps.size() != functionSymbol.paramTypeList.size()){
                    errorRecorder.addError(ErrorType.NUM_OF_PARAM_NOT_MATCH, unaryExp.identLineNum);
                    return visitResult;
                }

                // 校验参数类型
                for (int i = 0; i < functionSymbol.paramTypeList.size(); i++){
                    if (!functionSymbol.paramTypeList.get(i).equals(r.paramTypes.get(i))){
                        errorRecorder.addError(ErrorType.TYPE_OF_PARAM_NOT_MATCH, unaryExp.identLineNum);
                    }
                }
            }else {
                if(!functionSymbol.paramTypeList.isEmpty()){
                    errorRecorder.addError(ErrorType.NUM_OF_PARAM_NOT_MATCH, unaryExp.identLineNum);
                    return visitResult;
                }
            }
            return visitResult;
        } else if (unaryExp.getUType() == 3) {
            // UnaryExp → UnaryOp UnaryExp
            VisitResult visitResult = new VisitResult();
            VisitResult r = visitUnaryExp(unaryExp.unaryExp);
            Integer val = r.constVal;

            assert r.expType != null;
            visitResult.expType = r.expType;

            if (val != null){
                TokenType op = visitUnaryOp(unaryExp.unaryOp);
                if (op == TokenType.PLUS){
                    visitResult.constVal = val;
                } else if (op == TokenType.MINU) {
                    visitResult.constVal = -val;
                }else {
                    visitResult.constVal = val == 0 ? 0 : 1;
                }
            }
            return visitResult;
        }else {
            return new VisitResult();
        }
    }

    // 单目运算符 UnaryOp → '+' | '−' | '!'
    public TokenType visitUnaryOp(UnaryOp unaryOp){
        return unaryOp.op;
    }

    // 基本表达式 PrimaryExp → '(' Exp ')' | LVal | Number
    public VisitResult visitPrimaryExp(PrimaryExp primaryExp){
        if (primaryExp.getUType() == 1){
            return visitExp(primaryExp.exp);
        } else if (primaryExp.getUType() == 2) {
            // PrimaryExp → LVal
            VisitResult r = visitLVal(primaryExp.lVal);
            return r;
        } else if (primaryExp.getUType() == 3) {
            return visitNumber(primaryExp.number);
        }else {
            return new VisitResult();
        }
    }

    // 左值表达式 LVal → Ident ['[' Exp ']']
    public VisitResult visitLVal(LVal lVal){
        VisitResult visitResult = new VisitResult();

        Symbol symbol = currentSymbolTable.getSymbol(lVal.ident);

        if (symbol == null){
            errorRecorder.addError(ErrorType.UNDEFINED_NAME, lVal.identLineNum);
            visitResult.expType.type = "int";
            return visitResult;
        }

        VarSymbol varSymbol = (VarSymbol) symbol;

        // 数组维度信息收集
        List<Integer> accessDims = new ArrayList<>();
        for (Exp exp : lVal.dimensions){
            VisitResult rtExp = visitExp(exp);
            accessDims.add(rtExp.constVal);
        }

        List<Integer> typeDims = new ArrayList<>();
        for (int i = accessDims.size(); i < varSymbol.varType.dims.size(); i++){
            typeDims.add(varSymbol.varType.dims.get(i));
        }
        if(!typeDims.isEmpty()){
            typeDims.set(0, null);
        }

        // 构建左值类型
        Type type = new Type();
        type.type = "int";
        type.dims.addAll(typeDims);
        visitResult.expType = type;

        if (varSymbol.isConst){
            if (!varSymbol.isArray()){
                visitResult.constVal = varSymbol.values.get(0); // 非数组常量，直接取初始值
            }else {
                int accessIndex = 0;
                int stride = 1;
                boolean validConst = true;
                // 计算数组常量的访问索引（多维数组转一维索引）
                for (int i = accessDims.size() - 1, j = varSymbol.varType.dims.size() - 1; i >= 0; i--, j--){
                    Integer accessDim = accessDims.get(i);
                    if (accessDim == null){
                        validConst = false;
                        break;
                    }
                    accessIndex += accessDim * stride;
                    stride *= varSymbol.varType.dims.get(j);
                }
                if (validConst){
                    // 取索引对应的数组元素值
                    visitResult.constVal = varSymbol.values.get(accessIndex);
                }
            }
        }
        return visitResult;
    }

    // 数值 Number → IntConst
    public VisitResult visitNumber(Number number){
        VisitResult visitResult = new VisitResult();
        visitResult.expType.type = "int";
        visitResult.constVal = Integer.parseInt(number.intConst);
        //visitResult.irValue = new ImmediateValue(visitResult.constVal);
        return visitResult;
    }

    // 常量表达式 ConstExp → AddExp
    public VisitResult visitConstExp(ConstExp constExp){
        VisitResult visitResult = visitAddExp(constExp.addExp);
        assert visitResult.constVal != null;
        return visitResult;
    }

    // 基本类型 BType → 'int'
    public String visitBType(BType bType){
        return "int";
    }

    // 函数定义 FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
    public void visitFuncDef(FuncDef funcDef){
        if (currentSymbolTable.contains(funcDef.ident)){
            errorRecorder.addError(ErrorType.NAME_REDEFINE, funcDef.identLineNum);
            return;
        }
        FunctionSymbol functionSymbol = new FunctionSymbol();
        functionSymbol.ident = funcDef.ident;

        Type retType = visitFuncType(funcDef.funcType);
        functionSymbol.retType = retType;

        // 创建函数局部作用域
        currentSymbolTable = currentSymbolTable.createSubTable();

        if (funcDef.funcFParams != null){
            functionSymbol.paramTypeList.addAll(visitFuncFParams(funcDef.funcFParams));
        }

        // 将函数符号插入父作用域
        currentSymbolTable.getPreTable().insertSymbol(functionSymbol);

        // 标记是否需要返回值
        isRetExpNotNeed = functionSymbol.retType.type.equals("void");

        visitBlock(funcDef.block);

        // 检查非 void 函数的返回值是否缺失
        if(!functionSymbol.retType.type.equals("void") && funcDef.block.isWithoutReturn()){
            errorRecorder.addError(ErrorType.RETURN_MISS, funcDef.block.blockRLineNum);
        }

        // 恢复父作用域
        currentSymbolTable = currentSymbolTable.getPreTable();
    }

    // 函数类型 FuncType → 'void' | 'int'
    public Type visitFuncType(FuncType funcType){
        Type retType = new Type();
        retType.type = funcType.type.toString();
        return retType;
    }

    // 函数形参表 FuncFParams → FuncFParam { ',' FuncFParam }
    public ArrayList<Type> visitFuncFParams(FuncFParams funcFParams){
        ArrayList<Type> types = new ArrayList<>();
        for(FuncFParam fParam : funcFParams.funcFParams){
            Type paramType = visitFuncFParam(fParam);
            if(paramType != null){
                types.add(paramType);
            }
        }
        return types;
    }

    // 函数形参 FuncFParam → BType Ident ['[' ']']
    public Type visitFuncFParam(FuncFParam fParam){
        if(currentSymbolTable.contains(fParam.ident)){
            errorRecorder.addError(ErrorType.NAME_REDEFINE, fParam.identLineNum);
            return null;
        }

        VarSymbol varSymbol = new VarSymbol();
        varSymbol.ident = fParam.ident;
        varSymbol.isConst = false;
        varSymbol.varType.type = visitBType(fParam.type);

        for (int i = 0; i < fParam.count; i++) {
            varSymbol.varType.dims.add(null);
        }

        currentSymbolTable.insertSymbol(varSymbol);

        Type rt = new Type();
        rt.type = "int";
        rt.dims.addAll(varSymbol.varType.dims);
        return rt;
    }

    // 函数实参表达式 FuncRParams → Exp { ',' Exp }
    public VisitResult visitFuncRParams(FuncRParams funcRParams){
        VisitResult visitResult = new VisitResult();
        for(Exp exp : funcRParams.exps){
            VisitResult r = visitExp(exp);
            visitResult.paramTypes.add(r.expType);
            //visitResult.irValues.add(r.irValue);
        }
        return visitResult;
    }

    // 语句块 Block → '{' { BlockItem } '}'
    public void visitBlock(Block block){
        for(BlockItem blockItem : block.blockItems){
            visitBlockItem(blockItem);
        }
    }

    // 语句块项 BlockItem → Decl | Stmt
    public void visitBlockItem(BlockItem blockItem){
        if (blockItem.getUType() == 1){
            visitDecl(blockItem.decl);
        }else if (blockItem.getUType() == 2){
            visitStmt(blockItem.stmt);
        }
    }

    /**
     * 语句 Stmt → LVal '=' Exp ';'
     * | [Exp] ';'
     * | Block
     * | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
     * | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
     * | 'break' ';'
     * | 'continue' ';'
     * | 'return' [Exp] ';'
     * | 'printf''('StringConst {','Exp}')'';'
     */
    public void visitStmt(Stmt stmt){
         if (stmt.getUType() == 1){
             // Stmt → LVal '=' Exp ';'
             VisitResult rtLVal = visitLVal(stmt.lVal);
             Symbol lValSymbol = currentSymbolTable.getSymbol(stmt.lVal.ident);
             if(lValSymbol instanceof VarSymbol lValVarSym && lValVarSym.isConst){
                 errorRecorder.addError(ErrorType.CHANGE_CONST_VAL, stmt.lVal.identLineNum);
             }

             VisitResult rtExp = visitExp(stmt.exp1);
         }else if (stmt.getUType() == 2){
             // Stmt → [Exp] ';'
             if(stmt.exp2 != null){
                 visitExp(stmt.exp2);
             }
         }else if (stmt.getUType() == 3){
             // Stmt → Block
             currentSymbolTable = currentSymbolTable.createSubTable();
             visitBlock(stmt.block);
             currentSymbolTable = currentSymbolTable.getPreTable();
         }else if (stmt.getUType() == 4){
             // Stmt → 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
             var r = visitCond(stmt.cond1);
             visitStmt(stmt.ifStmt);

             if(stmt.elseStmt != null){
                 visitStmt(stmt.elseStmt);
             }
         }else if (stmt.getUType() == 5){
             // Stmt → 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
            breakBrInsts.push(new ArrayList<>());
            continueBrInsts.push(new ArrayList<>());

            if(stmt.forStmt1 != null){
                visitForStmt(stmt.forStmt1);
            }

            VisitResult condRt = new VisitResult();
            if(stmt.cond2 != null){
                condRt = visitCond(stmt.cond2);
            }

            isInLoop++;
            visitStmt(stmt.stmt);
            isInLoop--;

            if(stmt.forStmt2 != null){
                visitForStmt(stmt.forStmt2);
            }

            continueBrInsts.pop();
            breakBrInsts.pop();
         }else if (stmt.getUType() == 6){
             // Stmt -> 'break' ';' Stmt -> 'continue' ';'
             if(isInLoop == 0){
                 errorRecorder.addError(ErrorType.BREAK_OR_CONTINUE_ERROR, stmt.tkLineNum);
                 return;
             }
         }else if (stmt.getUType() == 7){
             // Stmt -> 'return' [Exp] ';'
             if(stmt.exp3 != null){
                 if(isRetExpNotNeed){
                     errorRecorder.addError(ErrorType.RETURN_NOT_MATCH, stmt.returnLineNum);
                     return;
                 }
                 var result = visitExp(stmt.exp3);
             }
         }else if (stmt.getUType() == 8){
             // Stmt -> 'printf''('StringConst {','Exp}')'';'
            for (Exp exp : stmt.exps){
                visitExp(exp);
            }

            // 校验格式化字符串 %d 个数与表达式个数匹配
            var fCharNum = (stmt.stringConst.length() - String.join("", stmt.stringConst.split("%d")).length()) / 2;
            if(fCharNum != stmt.exps.size()){
                errorRecorder.addError(ErrorType.PRINTF_NOT_MATCH, stmt.printfLineNum);
                return;
            }
         }
    }

    // 条件表达式 Cond → LOrExp
    public VisitResult visitCond(Cond cond){
        return visitLOrExp(cond.lOrExp);
    }

    // 语句 ForStmt → LVal '=' Exp { ',' LVal '=' Exp }
    public void visitForStmt(ForStmt stmt){
        for(int i = 0; i < stmt.lVals.size(); i++){
            visitLVal(stmt.lVals.get(i));
            visitExp(stmt.exps.get(i));

            Symbol lValSymbol = currentSymbolTable.getSymbol(stmt.lVals.get(i).ident);
            if(lValSymbol instanceof VarSymbol lValVarSym && lValVarSym.isConst){
                errorRecorder.addError(ErrorType.CHANGE_CONST_VAL, stmt.lVals.get(i).identLineNum);
            }
        }
    }

    // 逻辑或表达式 LOrExp → LAndExp | LOrExp '||' LAndExp
    public VisitResult visitLOrExp(LOrExp lOrExp){
        if(lOrExp.getUType() == 1){
            VisitResult visitResult = new VisitResult();
            VisitResult r = visitLAndExp(lOrExp.lAndExp1);
            visitResult.expType = r.expType;
            return visitResult;
        } else if (lOrExp.getUType() == 2) {
            VisitResult visitResult = new VisitResult();

            VisitResult r1 = visitLOrExp(lOrExp.lOrExp);
            VisitResult r2 = visitLAndExp(lOrExp.lAndExp2);

            visitResult.expType = r1.expType;

            return visitResult;
        }else {
            return new VisitResult();
        }
    }

    // 逻辑与表达式 LAndExp → EqExp | LAndExp '&&' EqExp
    public VisitResult visitLAndExp(LAndExp lAndExp){
        if(lAndExp.getUType() == 1){
            VisitResult visitResult = new VisitResult();

            VisitResult r = visitEqExp(lAndExp.eqExp1);
            visitResult.expType = r.expType;
            return visitResult;
        } else if (lAndExp.getUType() == 2) {
            VisitResult visitResult = new VisitResult();

            VisitResult r1 = visitLAndExp(lAndExp.lAndExp);
            VisitResult r2 = visitEqExp(lAndExp.eqExp2);

            visitResult.expType = r1.expType;
            return visitResult;
        }else {
            return new VisitResult();
        }
    }

    // 相等性表达式 EqExp → RelExp | EqExp ('==' | '!=') RelExp
    public VisitResult visitEqExp(EqExp eqExp){
        if(eqExp.getUType() == 1){
            return visitRelExp(eqExp.relExp1);
        } else if (eqExp.getUType() == 2) {
            VisitResult visitResult = new VisitResult();
            VisitResult r1 = visitEqExp(eqExp.eqExp);
            VisitResult r2 = visitRelExp(eqExp.relExp2);

            visitResult.expType = r1.expType;
            return visitResult;
        }else {
            return new VisitResult();
        }
    }

    // 关系表达式 RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    public VisitResult visitRelExp(RelExp relExp){
        if(relExp.getUType() == 1){
            return visitAddExp(relExp.addExp1);
        }else if (relExp.getUType() == 2) {
            VisitResult visitResult = new VisitResult();
            VisitResult r1 = visitRelExp(relExp.relExp);
            VisitResult r2 = visitAddExp(relExp.addExp2);

            visitResult.expType = r1.expType;
            return visitResult;
        }else {
            return new VisitResult();
        }
    }

    // 主函数定义 MainFuncDef → 'int' 'main' '(' ')' Block
    public void visitMainFuncDef(MainFuncDef mainFuncDef){
//        FunctionSymbol functionSymbol = new FunctionSymbol();
//        functionSymbol.retType.type = "int";
//        functionSymbol.ident = "main";
//        currentSymbolTable.insertSymbol(functionSymbol);

        // 进入main函数局部作用域
        currentSymbolTable = currentSymbolTable.createSubTable();

        isRetExpNotNeed = false;

        visitBlock(mainFuncDef.block);

        if (mainFuncDef.block.isWithoutReturn()){
            errorRecorder.addError(ErrorType.RETURN_MISS, mainFuncDef.block.blockRLineNum);
        }

        // 退出局部作用域
        currentSymbolTable = currentSymbolTable.getPreTable();
    }
}
