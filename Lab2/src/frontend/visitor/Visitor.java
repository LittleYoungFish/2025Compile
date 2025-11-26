package frontend.visitor;

import backend.ir.*;
import backend.ir.Module;
import backend.ir.inst.AllocInst;
import backend.ir.inst.BrInst;
import backend.ir.inst.ICmpInst;
import backend.ir.inst.ICmpInstCond;
import error.ErrorRecorder;
import error.ErrorType;
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
import frontend.symtable.SymbolTable;
import frontend.symtable.symbol.FunctionSymbol;
import frontend.symtable.symbol.Symbol;
import frontend.symtable.symbol.Type;
import frontend.symtable.symbol.VarSymbol;

import java.util.*;

public class Visitor {
    /** 创建符号表 **/
    private final ErrorRecorder errorRecorder;
    // 全局根符号表
    private final SymbolTable symbolTable = new SymbolTable();
    private SymbolTable currentSymbolTable = symbolTable;
    private int isInLoop = 0;
    private boolean isRetExpNotNeed = false;

    private final Stack<List<BrInst>> continueBrInsts = new Stack<>();
    private final Stack<List<BrInst>> breakBrInsts = new Stack<>();

    private static final Set<String> LIBRARY_FUNCTIONS = new HashSet<>(Arrays.asList("getint", "printf"));

    protected DeclVisitor declVisitor;
    protected ExpVisitor expVisitor;
    protected FuncVisitor funcVisitor;
    protected StmtVisitor stmtVisitor;
    protected VarVisitor varVisitor;

    /** 生成中间代码 **/
    private Module irModule = new Module();
    private Function currFunction = null;
    private BasicBlock currBasicBlock = null;
    // 静态变量序号计数器
    private Map<String, Integer> staticVarCounter = new HashMap<>();
    // 记录已生成的静态全局变量，避免重复创建
    private Map<String, GlobalValue> staticGlobalMap = new HashMap<>();

    private boolean isGlobalVar = true;

    // 生成中间代码
    public Module generateIR(Node root){
        if (root instanceof CompUnit compUnit){
            visitCompUnit(compUnit);
            return irModule;
        }else {
            return null;
        }
    }

    public Visitor(ErrorRecorder errorRecorder) {
        this.errorRecorder = errorRecorder;

        this.declVisitor = new DeclVisitor(this);
        this.expVisitor = new ExpVisitor(this);
        this.funcVisitor = new FuncVisitor(this);
        this.stmtVisitor = new StmtVisitor(this);
        this.varVisitor = new VarVisitor(this);

        initLibraryFunctions();
    }

    // 初始化符号表（把库函数先加入符号表）
    private void initLibraryFunctions() {
        for (String funcName : LIBRARY_FUNCTIONS) {
            FunctionSymbol funcSymbol = new FunctionSymbol();
            funcSymbol.ident = funcName;
            funcSymbol.retType.type = "int";

            // 为库函数生成IR层的Function对象
            if (funcName.equals("getint")) {
                List<IRType> paramTypes = new ArrayList<>();
                IRType retType = IRType.getInt();
                Function func = irModule.createFunction(retType, paramTypes);
                func.setName("getint");
                func.setLibrary(true);
                funcSymbol.targetValue = func;
                symbolTable.insertSymbol(funcSymbol);
            }else {
                List<IRType> paramTypes = new ArrayList<>();
                IRType retType = IRType.getInt(); // 返回int
                Function func = irModule.createFunction(retType, paramTypes);
                func.setName(funcName);
                func.setLibrary(true);

                // 将IR函数关联到符号表，确保调用时能找到
                funcSymbol.targetValue = func;

                symbolTable.insertSymbol(funcSymbol);
            }
        }
    }

    // 编译单元 CompUnit → {Decl} {FuncDef} MainFuncDef
    public SymbolTable visitCompUnit(CompUnit compUnit) {
        isGlobalVar = true;
        for (Decl decl : compUnit.decls){
            declVisitor.visitDecl(decl);
        }
        isGlobalVar = false;

        for(FuncDef funcDef : compUnit.funcDefs){
            funcVisitor.visitFuncDef(funcDef);
        }

        declVisitor.visitMainFuncDef(compUnit.mainFuncDef);

        return symbolTable;
    }

    public SymbolTable getCurrentSymbolTable(){
        return currentSymbolTable;
    }

    public Module getIrModule() {
        return irModule;
    }

    public BasicBlock getCurrBasicBlock() {
        return currBasicBlock;
    }

    public ErrorRecorder getErrorRecorder() {
        return errorRecorder;
    }

    public Function getCurrFunction() {
        return currFunction;
    }

    public int getIsInLoop() {
        return isInLoop;
    }

    public boolean isGlobalVar(){
        return isGlobalVar;
    }

    public ExpVisitor getExpVisitor() {
        return expVisitor;
    }

    public DeclVisitor getDeclVisitor() {
        return declVisitor;
    }

    public FuncVisitor getFuncVisitor() {
        return funcVisitor;
    }

    public StmtVisitor getStmtVisitor() {
        return stmtVisitor;
    }

    public VarVisitor getVarVisitor() {
        return varVisitor;
    }

    public Map<String, GlobalValue> getStaticGlobalMap() {
        return staticGlobalMap;
    }

    public Map<String, Integer> getStaticVarCounter() {
        return staticVarCounter;
    }

    public boolean isRetExpNotNeed() {
        return isRetExpNotNeed;
    }

    public Stack<List<BrInst>> getBreakBrInsts() {
        return breakBrInsts;
    }

    public Stack<List<BrInst>> getContinueBrInsts() {
        return continueBrInsts;
    }

    public void setIsGlobalVar(boolean isGlobalVar) {
        this.isGlobalVar = isGlobalVar;
    }

    public void setIsInLoop(int isInLoop) {
        this.isInLoop = isInLoop;
    }

    public void setRetExpNotNeed(boolean retExpNotNeed) {
        this.isRetExpNotNeed = retExpNotNeed;
    }

    public void setCurrentSymbolTable(SymbolTable currentSymbolTable) {
        this.currentSymbolTable = currentSymbolTable;
    }

    public void setCurrFunction(Function currFunction) {
        this.currFunction = currFunction;
    }

    public void setCurrBasicBlock(BasicBlock currBasicBlock) {
        this.currBasicBlock = currBasicBlock;
    }

    public void incrementIsInLoop() {
        this.isInLoop++;
    }

    public void decrementIsInLoop() {
        this.isInLoop--;
    }

//    // 声明 Decl → ConstDecl | VarDecl
//    public void visitDecl(Decl decl) {
//        if (decl.getUType() == 1){
//            visitConstDecl(decl.constDecl);
//        } else if (decl.getUType() == 2) {
//            visitVarDecl(decl.varDecl);
//        }
//    }
//
//    // 常量声明 ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
//    public void visitConstDecl(ConstDecl constDecl) {
//        for (ConstDef constDef : constDecl.constDefs){
//            visitConstDef(constDef);
//        }
//    }
//
//    // 常量定义 ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
//    public void visitConstDef(ConstDef constDef) {
//        if(currentSymbolTable.contains(constDef.ident)){
//            errorRecorder.addError(ErrorType.NAME_REDEFINE, constDef.identLineNum);
//            return;
//        }
//
//        // 常量符号创建
//        VarSymbol varSymbol = new VarSymbol();
//        varSymbol.ident = constDef.ident;
//        varSymbol.isConst = true;
//        varSymbol.varType.type = "int";
//
//        // 若为数组常量
//        for (ConstExp dimension : constDef.dimensions){
//            varSymbol.varType.dims.add(visitConstExp(dimension).constVal);
//        }
//
//        // 常量初始化提取
//        VisitResult r = visitConstInitVal(constDef.constInitVal);
//        varSymbol.values.addAll(r.constInitVals);
//        List<Value> initValues = r.irValues; // 初始值的IR表示
//
//        if (isGlobalVar){
//            GlobalValue globalValue = irModule.createGlobalValue(IRType.getInt().dims(varSymbol.varType.dims), varSymbol.values);
//            globalValue.setName(varSymbol.ident);
//            varSymbol.targetValue = globalValue; // 符号表关联IR对象
//        }else {
//            // 分配局部变量空间
//            Value localVar = currFunction.getFirstBasicBlock().createAllocInstAndInsert(IRType.getInt().dims(varSymbol.varType.dims));
//            varSymbol.targetValue = localVar;
//
//            if(!varSymbol.isArray()){
//                // 非数组常量直接赋值
//                currBasicBlock.createStoreInst(initValues.get(0), varSymbol.targetValue);
//            }else {
//                // array with init values
//                // 数组常量逐个元素赋值
//                for (int i = 0; i < initValues.size(); i++){
//                    // 计算第i个元素的多维索引
//                    int[] indexs = new int[varSymbol.varType.dims.size()];
//                    int pos = i;
//                    for (int j = indexs.length - 1; j >= 0; j--){
//                        indexs[j] = pos % varSymbol.varType.dims.get(j);
//                        pos /= varSymbol.varType.dims.get(j);
//                    }
//
//                    // 生成GEP指令获取数组元素的地址
//                    Value initValue = initValues.get(i);
//                    Value arrayPtr = currBasicBlock.createGetElementPtrInst(varSymbol.targetValue, List.of(new ImmediateValue(0), new ImmediateValue(0)));
//                    for (int j = 0; j < indexs.length; j++){
//                        int visitIdx = indexs[j];
//                        List<Value> offsets = j == indexs.length - 1 ? List.of(new ImmediateValue(visitIdx)) : List.of(new ImmediateValue(visitIdx), new ImmediateValue(0));
//                        arrayPtr = currBasicBlock.createGetElementPtrInst(arrayPtr, offsets);
//                    }
//                    currBasicBlock.createStoreInst(initValue, arrayPtr);
//                }
//            }
//        }
//
//        currentSymbolTable.insertSymbol(varSymbol);
//    }

//    // 常量初值 ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}'
//    public VisitResult visitConstInitVal(ConstInitVal constInitVal){
//        if(constInitVal.getUType() == 1){
//            //ConstInitVal → ConstExp
//            VisitResult visitResult = new VisitResult();
//            var r = visitConstExp(constInitVal.constExp);
//            visitResult.constInitVals.add(r.constVal);
//            // 不使用addAll，addAll用于 const int arr[2][2] = {{1,2}, {3,4}}，内层的 {1,2} 和 {3,4} 本身也是 ConstInitVal
//            visitResult.irValues.add(r.irValue);
//            return visitResult;
//        } else if (constInitVal.getUType() == 2) {
//            //ConstInitVal → '{' [ ConstExp { ',' ConstExp } ] '}'
//            VisitResult visitResult = new VisitResult();
//            for (ConstExp constExp : constInitVal.constExps){
//                var r = visitConstExp(constExp);
//                visitResult.constInitVals.add(r.constVal);
//                visitResult.irValues.add(r.irValue);
//            }
//            return visitResult;
//        }else {
//            return new VisitResult();
//        }
//    }

//    // 变量声明 VarDecl → [ 'static' ] BType VarDef { ',' VarDef } ';'
//    public void visitVarDecl(VarDecl varDecl){
//        visitBType(varDecl.type);
//
//        boolean isStatic = varDecl.getUType() == 1;
//
//        for (VarDef varDef : varDecl.varDefs){
//            visitVarDef(varDef, isStatic);
//        }
//    }
//
//    // 变量定义 VarDef → Ident [ '[' ConstExp ']' ] | Ident [ '[' ConstExp ']' ] '=' InitVal
//    public void visitVarDef(VarDef varDef, boolean isStatic){
//        if (currentSymbolTable.contains(varDef.ident)){
//            errorRecorder.addError(ErrorType.NAME_REDEFINE, varDef.identLineNum);
//            return;
//        }
//
//        VarSymbol varSymbol = new VarSymbol();
//        varSymbol.isConst = false;
//        varSymbol.isStatic = isStatic;
//        varSymbol.ident = varDef.ident;
//        varSymbol.varType.type = "int";
//
//        for (ConstExp dimension : varDef.dimensions){
//            varSymbol.varType.dims.add(visitConstExp(dimension).constVal);
//        }
//
//        if(isGlobalVar){
//            if (varDef.initVal !=null){
//                VisitResult r = visitInitVal(varDef.initVal);
//                varSymbol.values.addAll(r.constInitVals);
//            }
//            GlobalValue globalValue = irModule.createGlobalValue(IRType.getInt().dims(varSymbol.varType.dims), varSymbol.values);
//            globalValue.setName(varSymbol.ident);
//            varSymbol.targetValue = globalValue;
//        }else {
//            // 非全局变量
//            if(varSymbol.isStatic){
//                // 静态局部变量
//                // 1. 生成唯一命名的key（函数名_变量名）
//                String funcName = currFunction.getName().substring(1); // 原逻辑：去掉前缀下划线
//                String baseKey = funcName + "_" + varSymbol.ident;
//                // 2. 获取序号：首次为0（命名为main_a），后续递增（main_a_1/main_a_2）
//                int seq = staticVarCounter.getOrDefault(baseKey, 0);
//                String staticGlobalName = baseKey + (seq == 0 ? "" : "_" + seq); // 核心：动态生成唯一名称
//                staticVarCounter.put(baseKey, seq + 1); // 更新序号
//
//                // 3. 检查缓存：避免重复创建全局值（解决重定义核心）
//                GlobalValue staticGlobal;
//                if (staticGlobalMap.containsKey(staticGlobalName)) {
//                    staticGlobal = staticGlobalMap.get(staticGlobalName);
//                } else {
//                    // 静态局部变量无维度，dims 为空列表
//                    List<Integer> initVals = new ArrayList<>();
//                    // 若有显式初始化值则用显式值，无则默认 0（这里先加 0，后续初始化逻辑会覆盖）
//                    if (varDef.initVal != null) {
//                        VisitResult r = visitInitVal(varDef.initVal);
//                        initVals.addAll(r.constInitVals);
//                    } else {
//                        initVals.add(0); // 静态变量默认初始化为 0
//                    }
//                    // 创建全局值（全局数据区存储）
//                    staticGlobal = irModule.createGlobalValue(IRType.getInt().dims(varSymbol.varType.dims), initVals);
//                    staticGlobal.setName(staticGlobalName);
//                    staticGlobalMap.put(staticGlobalName, staticGlobal);
//                }
//                varSymbol.targetValue = staticGlobal;
//
//                // 若有显式初始化，生成 store 指令覆盖默认值
//                if (varDef.initVal != null) {
//                    //VisitResult r = visitInitVal(varDef.initVal);
//                    // 静态局部变量初始化：全局值仅需赋值一次，直接生成 store 到全局值地址
//                    //currBasicBlock.createStoreInst(r.irValues.get(0), varSymbol.targetValue);
//                }
//            }else {
//                Value localVar = currFunction.getFirstBasicBlock().createAllocInstAndInsert(IRType.getInt().dims(varSymbol.varType.dims));
//                varSymbol.targetValue = localVar;
//                if (varDef.initVal != null) {
//                    VisitResult r = visitInitVal(varDef.initVal);
//                    List<Value> initValues = r.irValues;
//                    if (!varSymbol.isArray()) {
//                        currBasicBlock.createStoreInst(r.irValues.get(0), varSymbol.targetValue);
//                    } else {
//                        for (int i = 0; i < initValues.size(); i++) {
//                            int[] indexs = new int[varSymbol.varType.dims.size()];
//                            int pos = i;
//                            for (int j = indexs.length - 1; j >= 0; j--) {
//                                indexs[j] = pos % varSymbol.varType.dims.get(j);
//                                pos /= varSymbol.varType.dims.get(j);
//                            }
//
//                            Value initValue = initValues.get(i);
//                            Value arrayPtr = currBasicBlock.createGetElementPtrInst(varSymbol.targetValue, List.of(new ImmediateValue(0), new ImmediateValue(0)));
//                            for (int j = 0; j < indexs.length; j++) {
//                                int visitIdx = indexs[j];
//                                List<Value> offsets = j == indexs.length - 1 ? List.of(new ImmediateValue(visitIdx)) : List.of(new ImmediateValue(visitIdx), new ImmediateValue(0));
//                                arrayPtr = currBasicBlock.createGetElementPtrInst(arrayPtr, offsets);
//                            }
//                            currBasicBlock.createStoreInst(initValue, arrayPtr);
//                        }
//                    }
//                }
//            }
//        }
//
//        currentSymbolTable.insertSymbol(varSymbol);
//    }

//    // 变量初值 InitVal → Exp | '{' [ Exp { ',' Exp } ] '}'
//    public VisitResult visitInitVal(InitVal initVal){
//        if (initVal.getUType() == 1){
//            // InitVal → Exp
//            VisitResult visitResult = new VisitResult();
//            var r = visitExp(initVal.exp);
//            visitResult.constInitVals.add(r.constVal);
//            visitResult.irValues.add(r.irValue);
//            return visitResult;
//        } else if (initVal.getUType() == 2) {
//            // InitVal → '{' [ Exp { ',' Exp } ] '}'
//            VisitResult visitResult = new VisitResult();
//            for (Exp exp : initVal.exps){
//                var r = visitExp(exp);
//                visitResult.constInitVals.add(r.constVal);
//                visitResult.irValues.add(r.irValue);
//            }
//            return visitResult;
//        }else {
//            return new VisitResult();
//        }
//    }

//    // 表达式 Exp → AddExp
//    public VisitResult visitExp(Exp exp){
//        return visitAddExp(exp.addExp);
//    }
//
//    // 加减表达式 AddExp → MulExp | AddExp ('+' | '−') MulExp
//    public VisitResult visitAddExp(AddExp addExp){
//        if (addExp.getUType() == 1){
//            return visitMulExp(addExp.mulExp1);
//        } else if (addExp.getUType() == 2) {
//            VisitResult visitResult = new VisitResult();
//            VisitResult r1= visitAddExp(addExp.addExp);
//            VisitResult r2 = visitMulExp(addExp.mulExp2);
//            Integer val1 = r1.constVal;
//            Integer val2 = r2.constVal;
//
//            assert r1.expType.equals(r2.expType);
//            visitResult.expType = r1.expType;
//
//            if (val1 != null && val2 != null){
//                if(addExp.op == TokenType.PLUS){
//                    visitResult.constVal = val1 + val2;
//                }else {
//                    visitResult.constVal = val1 - val2;
//                }
//            }
//            if (currBasicBlock != null){
//                if (addExp.op == TokenType.PLUS){
//                    visitResult.irValue = currBasicBlock.createAddInst(r1.irValue, r2.irValue);
//                }else {
//                    visitResult.irValue = currBasicBlock.createSubInst(r1.irValue, r2.irValue);
//                }
//            }
//            return visitResult;
//        }else {
//            return new VisitResult();
//        }
//    }
//
//    // 乘除模表达式 MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
//    public VisitResult visitMulExp(MulExp mulExp){
//        if(mulExp.getUType() == 1){
//            return visitUnaryExp(mulExp.unaryExp1);
//        }else if(mulExp.getUType() == 2){
//            VisitResult visitResult = new VisitResult();
//            VisitResult r1 = visitMulExp(mulExp.mulExp);
//            VisitResult r2 = visitUnaryExp(mulExp.unaryExp2);
//            Integer val1 = r1.constVal;
//            Integer val2 = r2.constVal;
//
//            assert r1.expType.equals(r2.expType);
//            visitResult.expType = r1.expType;
//
//            if (val1 != null && val2 != null){
//                if(mulExp.op == TokenType.MULT){
//                    visitResult.constVal = val1 * val2;
//                } else if (mulExp.op == TokenType.DIV) {
//                    visitResult.constVal = val1 / val2;
//                }else if (mulExp.op == TokenType.MOD) {
//                    visitResult.constVal = val1 % val2;
//                }
//            }
//
//            if (currBasicBlock != null){
//                if (mulExp.op == TokenType.MULT){
//                    visitResult.irValue = currBasicBlock.createMulInst(r1.irValue, r2.irValue);
//                } else if (mulExp.op == TokenType.DIV) {
//                    visitResult.irValue = currBasicBlock.createDivInst(r1.irValue, r2.irValue);
//                } else if (mulExp.op == TokenType.MOD) {
//                    visitResult.irValue = currBasicBlock.createSRemInst(r1.irValue, r2.irValue);
//                }
//            }
//
//            return visitResult;
//        }else {
//            return new VisitResult();
//        }
//    }
//
//    // 一元表达式 UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
//    public VisitResult visitUnaryExp(UnaryExp unaryExp){
//        if (unaryExp.getUType() == 1){
//            return visitPrimaryExp(unaryExp.primaryExp);
//        } else if (unaryExp.getUType() == 2) {
//            // UnaryExp → Ident '(' [FuncRParams] ')'
//            VisitResult visitResult = new VisitResult();
//            Symbol symbol = currentSymbolTable.getSymbol(unaryExp.ident);
//
//            if (symbol == null){
//                errorRecorder.addError(ErrorType.UNDEFINED_NAME, unaryExp.identLineNum);
//                visitResult.expType.type = "int";
//                return visitResult;
//            }
//
//            FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
//
//            visitResult.expType = functionSymbol.retType;
//
//            if (unaryExp.funcRParams != null){
//                VisitResult r = visitFuncRParams(unaryExp.funcRParams);
//
//                // 校验参数个数
//                if(unaryExp.funcRParams.exps.size() != functionSymbol.paramTypeList.size()){
//                    errorRecorder.addError(ErrorType.NUM_OF_PARAM_NOT_MATCH, unaryExp.identLineNum);
//                    return visitResult;
//                }
//
//                // 校验参数类型
//                for (int i = 0; i < functionSymbol.paramTypeList.size(); i++){
//                    if (!functionSymbol.paramTypeList.get(i).equals(r.paramTypes.get(i))){
//                        errorRecorder.addError(ErrorType.TYPE_OF_PARAM_NOT_MATCH, unaryExp.identLineNum);
//                    }
//                }
//
//                visitResult.irValue = currBasicBlock.createCallInst((Function) functionSymbol.targetValue, r.irValues);
//            }else {
//                if(!functionSymbol.paramTypeList.isEmpty()){
//                    errorRecorder.addError(ErrorType.NUM_OF_PARAM_NOT_MATCH, unaryExp.identLineNum);
//                    return visitResult;
//                }
//                visitResult.irValue = currBasicBlock.createCallInst((Function) functionSymbol.targetValue, List.of());
//            }
//            return visitResult;
//        } else if (unaryExp.getUType() == 3) {
//            // UnaryExp → UnaryOp UnaryExp
//            VisitResult visitResult = new VisitResult();
//            VisitResult r = visitUnaryExp(unaryExp.unaryExp);
//            Integer val = r.constVal;
//
//            assert r.expType != null;
//            visitResult.expType = r.expType;
//
//            if (val != null){
//                TokenType op = visitUnaryOp(unaryExp.unaryOp);
//                if (op == TokenType.PLUS){
//                    visitResult.constVal = val;
//                } else if (op == TokenType.MINU) {
//                    visitResult.constVal = -val;
//                }else {
//                    visitResult.constVal = val == 0 ? 0 : 1;
//                }
//            }
//
//            if (currBasicBlock != null){
//                TokenType op = visitUnaryOp(unaryExp.unaryOp);
//                if (op == TokenType.PLUS){
//                    visitResult.irValue = r.irValue;
//                }else if(op == TokenType.MINU){
//                    visitResult.irValue = currBasicBlock.createSubInst(new ImmediateValue(0), r.irValue);
//                }else {
//                    //visitResult.irValue = currBasicBlock.createICmpInst(ICmpInstCond.EQ, new ImmediateValue(0), r.irValue);
//                    // 1. 第一步：比较 x == 0 → 得到 i1 类型（true 表示 x=0）
//                    Value icmp = currBasicBlock.createICmpInst(ICmpInstCond.EQ, new ImmediateValue(0), r.irValue);
//                    // 2. 第二步：将 i1 零扩展为 i32（true→1，false→0），符合 C 语言 !x 的语义
//                    Value zext = currBasicBlock.createZExtInst(IRType.getInt(), icmp); // IRType.getInt() 是 i32
//                    visitResult.irValue = zext;
//                }
//            }
//            return visitResult;
//        }else {
//            return new VisitResult();
//        }
//    }
//
//    // 单目运算符 UnaryOp → '+' | '−' | '!'
//    public TokenType visitUnaryOp(UnaryOp unaryOp){
//        return unaryOp.op;
//    }
//
//    // 基本表达式 PrimaryExp → '(' Exp ')' | LVal | Number
//    public VisitResult visitPrimaryExp(PrimaryExp primaryExp){
//        if (primaryExp.getUType() == 1){
//            return visitExp(primaryExp.exp);
//        } else if (primaryExp.getUType() == 2) {
//            // PrimaryExp → LVal
//            VisitResult r = visitLVal(primaryExp.lVal);
//            if (currBasicBlock != null){
//                if(r.lvalLoadNotNeed){
//                    return r;
//                }
//                r.irValue = currBasicBlock.createLoadInst(r.irValue);
//            }
//            return r;
//        } else if (primaryExp.getUType() == 3) {
//            return visitNumber(primaryExp.number);
//        }else {
//            return new VisitResult();
//        }
//    }

//    // 左值表达式 LVal → Ident ['[' Exp ']']
//    public VisitResult visitLVal(LVal lVal){
//        VisitResult visitResult = new VisitResult();
//
//        Symbol symbol = currentSymbolTable.getSymbol(lVal.ident);
//
//        if (symbol == null){
//            errorRecorder.addError(ErrorType.UNDEFINED_NAME, lVal.identLineNum);
//            visitResult.expType.type = "int";
//            return visitResult;
//        }
//
//        VarSymbol varSymbol = (VarSymbol) symbol;
//
//        // 数组维度信息收集
//        List<Integer> accessDims = new ArrayList<>();
//        List<Value> irVisitDims = new ArrayList<>();
//        for (Exp exp : lVal.dimensions){
//            VisitResult rtExp = visitExp(exp);
//            accessDims.add(rtExp.constVal);
//            irVisitDims.add(rtExp.irValue);
//        }
//
//        List<Integer> typeDims = new ArrayList<>();
//        for (int i = accessDims.size(); i < varSymbol.varType.dims.size(); i++){
//            typeDims.add(varSymbol.varType.dims.get(i));
//        }
//        if(!typeDims.isEmpty()){
//            typeDims.set(0, null);
//        }
//
//        // 构建左值类型
//        Type type = new Type();
//        type.type = "int";
//        type.dims.addAll(typeDims);
//        visitResult.expType = type;
//
//        if (varSymbol.isConst){
//            if (!varSymbol.isArray()){
//                visitResult.constVal = varSymbol.values.get(0); // 非数组常量，直接取初始值
//            }else {
//                int accessIndex = 0;
//                int stride = 1;
//                boolean validConst = true;
//                // 计算数组常量的访问索引（多维数组转一维索引）
//                for (int i = accessDims.size() - 1, j = varSymbol.varType.dims.size() - 1; i >= 0; i--, j--){
//                    Integer accessDim = accessDims.get(i);
//                    if (accessDim == null){
//                        validConst = false;
//                        break;
//                    }
//                    accessIndex += accessDim * stride;
//                    stride *= varSymbol.varType.dims.get(j);
//                }
//                if (validConst){
//                    // 取索引对应的数组元素值
//                    visitResult.constVal = varSymbol.values.get(accessIndex);
//                }
//            }
//        }
//
//        if (currBasicBlock != null){
//            if(varSymbol.isArray()){
//                List<Integer> dims = varSymbol.varType.dims;
//
//                Value arrayPtr;
//                Value symbolValue = varSymbol.targetValue;
//
//                if(symbolValue instanceof AllocInst allocSymVal && allocSymVal.getDataType().getPtrNum() != 0){
//                    arrayPtr = currBasicBlock.createLoadInst(symbolValue);
//                }else {
//                    arrayPtr = currBasicBlock.createGetElementPtrInst(symbolValue, List.of(new ImmediateValue(0), new ImmediateValue(0)));
//                }
//
//                for (int i = 0; i < irVisitDims.size(); i++){
//                    Value visitDim = irVisitDims.get(i);
//                    List<Value> offsets = (i == dims.size() - 1) ? List.of(visitDim) : List.of(visitDim, new ImmediateValue(0));
//                    arrayPtr = currBasicBlock.createGetElementPtrInst(arrayPtr, offsets);
//                }
//
//                visitResult.irValue = arrayPtr;
//                visitResult.lvalLoadNotNeed = dims.size() != irVisitDims.size();
//            }else {
//                if(varSymbol.isConst){
//                    visitResult.irValue = new ImmediateValue(varSymbol.values.get(0));
//                    visitResult.lvalLoadNotNeed = true;
//                }else {
//                    visitResult.irValue = varSymbol.targetValue;
//                }
//            }
//        }
//        return visitResult;
//    }

//    // 数值 Number → IntConst
//    public VisitResult visitNumber(Number number){
//        VisitResult visitResult = new VisitResult();
//        visitResult.expType.type = "int";
//        visitResult.constVal = Integer.parseInt(number.intConst);
//        visitResult.irValue = new ImmediateValue(visitResult.constVal);
//        return visitResult;
//    }

//    // 常量表达式 ConstExp → AddExp
//    public VisitResult visitConstExp(ConstExp constExp){
//        VisitResult visitResult = visitAddExp(constExp.addExp);
//        assert visitResult.constVal != null;
//        return visitResult;
//    }

//    // 基本类型 BType → 'int'
//    public String visitBType(BType bType){
//        return "int";
//    }

//    // 函数定义 FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
//    public void visitFuncDef(FuncDef funcDef){
//        if (currentSymbolTable.contains(funcDef.ident)){
//            errorRecorder.addError(ErrorType.NAME_REDEFINE, funcDef.identLineNum);
//            return;
//        }
//        FunctionSymbol functionSymbol = new FunctionSymbol();
//        functionSymbol.ident = funcDef.ident;
//
//        Type retType = visitFuncType(funcDef.funcType);
//        functionSymbol.retType = retType;
//
//        // 创建函数局部作用域
//        currentSymbolTable = currentSymbolTable.createSubTable();
//
//        if (funcDef.funcFParams != null){
//            functionSymbol.paramTypeList.addAll(visitFuncFParams(funcDef.funcFParams));
//        }
//
//        // 将函数符号插入父作用域
//        currentSymbolTable.getPreTable().insertSymbol(functionSymbol);
//
//        // 标记是否需要返回值
//        isRetExpNotNeed = functionSymbol.retType.type.equals("void");
//
//        ArrayList<IRType> irArgTypes = new ArrayList<>();
//        for (Type type : functionSymbol.paramTypeList){
//            if (type.dims.isEmpty()){
//                irArgTypes.add(IRType.getInt());
//            }else {
//                irArgTypes.add(IRType.getInt().dims(type.dims.subList(1, type.dims.size())).ptr(1));
//            }
//        }
//
//        currFunction = irModule.createFunction(functionSymbol.retType.type.equals("void") ? IRType.getVoid() : IRType.getInt(), irArgTypes);
//        currFunction.setName(functionSymbol.ident);
//        functionSymbol.targetValue = currFunction;
//        currBasicBlock = currFunction.createBasicBlock();
//        currBasicBlock.setLoopNum(isInLoop);
//
//        if (funcDef.funcFParams != null){
//            for (int i = currFunction.getArguments().size() - 1; i >= 0; i--){
//                FunctionArgument currArgVal = currFunction.getArguments().get(i);
//                Symbol currParamSym = currentSymbolTable.getSymbol(funcDef.funcFParams.funcFParams.get(i).ident);
//                Value currArgPtr = currFunction.getFirstBasicBlock().createAllocInstAndInsert(currArgVal.getType());
//                currBasicBlock.createStoreInst(currArgVal, currArgPtr);
//                currParamSym.targetValue = currArgPtr;
//            }
//        }
//
//        visitBlock(funcDef.block);
//
//        if (functionSymbol.retType.type.equals("void") && funcDef.block.isWithoutReturn()){
//            currBasicBlock.createReturnInst(null);
//        }
//
//        currBasicBlock = null;
//        currFunction = null;
//
//        // 检查非 void 函数的返回值是否缺失
//        if(!functionSymbol.retType.type.equals("void") && funcDef.block.isWithoutReturn()){
//            errorRecorder.addError(ErrorType.RETURN_MISS, funcDef.block.blockRLineNum);
//        }
//
//        // 恢复父作用域
//        currentSymbolTable = currentSymbolTable.getPreTable();
//    }
//
//    // 函数类型 FuncType → 'void' | 'int'
//    public Type visitFuncType(FuncType funcType){
//        Type retType = new Type();
//        retType.type = funcType.type.toString();
//        return retType;
//    }
//
//    // 函数形参表 FuncFParams → FuncFParam { ',' FuncFParam }
//    public ArrayList<Type> visitFuncFParams(FuncFParams funcFParams){
//        ArrayList<Type> types = new ArrayList<>();
//        for(FuncFParam fParam : funcFParams.funcFParams){
//            Type paramType = visitFuncFParam(fParam);
//            if(paramType != null){
//                types.add(paramType);
//            }
//        }
//        return types;
//    }
//
//    // 函数形参 FuncFParam → BType Ident ['[' ']']
//    public Type visitFuncFParam(FuncFParam fParam){
//        if(currentSymbolTable.contains(fParam.ident)){
//            errorRecorder.addError(ErrorType.NAME_REDEFINE, fParam.identLineNum);
//            return null;
//        }
//
//        VarSymbol varSymbol = new VarSymbol();
//        varSymbol.ident = fParam.ident;
//        varSymbol.isConst = false;
//        varSymbol.varType.type = visitBType(fParam.type);
//
//        for (int i = 0; i < fParam.count; i++) {
//            varSymbol.varType.dims.add(null);
//        }
//
//        currentSymbolTable.insertSymbol(varSymbol);
//
//        Type rt = new Type();
//        rt.type = "int";
//        rt.dims.addAll(varSymbol.varType.dims);
//        return rt;
//    }
//
//    // 函数实参表达式 FuncRParams → Exp { ',' Exp }
//    public VisitResult visitFuncRParams(FuncRParams funcRParams){
//        VisitResult visitResult = new VisitResult();
//        for(Exp exp : funcRParams.exps){
//            VisitResult r = visitExp(exp);
//            visitResult.paramTypes.add(r.expType);
//            visitResult.irValues.add(r.irValue);
//        }
//        return visitResult;
//    }

//    // 语句块 Block → '{' { BlockItem } '}'
//    public void visitBlock(Block block){
//        for(BlockItem blockItem : block.blockItems){
//            visitBlockItem(blockItem);
//        }
//    }
//
//    // 语句块项 BlockItem → Decl | Stmt
//    public void visitBlockItem(BlockItem blockItem){
//        if (blockItem.getUType() == 1){
//            visitDecl(blockItem.decl);
//        }else if (blockItem.getUType() == 2){
//            visitStmt(blockItem.stmt);
//        }
//    }
//
//    /**
//     * 语句 Stmt → LVal '=' Exp ';'
//     * | [Exp] ';'
//     * | Block
//     * | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
//     * | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
//     * | 'break' ';'
//     * | 'continue' ';'
//     * | 'return' [Exp] ';'
//     * | 'printf''('StringConst {','Exp}')'';'
//     */
//    public void visitStmt(Stmt stmt){
//         if (stmt.getUType() == 1){
//             // Stmt → LVal '=' Exp ';'
//             VisitResult rtLVal = visitLVal(stmt.lVal);
//             Symbol lValSymbol = currentSymbolTable.getSymbol(stmt.lVal.ident);
//             if(lValSymbol instanceof VarSymbol lValVarSym && lValVarSym.isConst){
//                 errorRecorder.addError(ErrorType.CHANGE_CONST_VAL, stmt.lVal.identLineNum);
//             }
//
//             VisitResult rtExp = visitExp(stmt.exp1);
//             currBasicBlock.createStoreInst(rtExp.irValue, rtLVal.irValue);
//         }else if (stmt.getUType() == 2){
//             // Stmt → [Exp] ';'
//             if(stmt.exp2 != null){
//                 visitExp(stmt.exp2);
//             }
//         }else if (stmt.getUType() == 3){
//             // Stmt → Block
//             currentSymbolTable = currentSymbolTable.createSubTable();
//             visitBlock(stmt.block);
//             currentSymbolTable = currentSymbolTable.getPreTable();
//         }else if (stmt.getUType() == 4){
//             // Stmt → 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
//             var r = visitCond(stmt.cond1);
//
//             BasicBlock trueBlock = currBasicBlock;
//             visitStmt(stmt.ifStmt);
//
//             BasicBlock lastBlockInTrue = currBasicBlock;
//             currBasicBlock = currFunction.createBasicBlock();
//             currBasicBlock.setLoopNum(isInLoop);
//
//             BasicBlock falseBlock = currBasicBlock;
//
//             if(stmt.elseStmt != null){
//                 visitStmt(stmt.elseStmt);
//                 BasicBlock lastBlockInFalse = currBasicBlock;
//                 currBasicBlock = currFunction.createBasicBlock();
//                 currBasicBlock.setLoopNum(isInLoop);
//
//                 lastBlockInFalse.createBrInstWithoutCond(currBasicBlock);
//             }
//             lastBlockInTrue.createBrInstWithoutCond(currBasicBlock);
//
//             for (BasicBlock blockToTrue : r.blocksToTrue){
//                 BrInst brInst = (BrInst) blockToTrue.getInstructions().get(blockToTrue.getInstructions().size() - 1);
//                 brInst.setTrueBranch(trueBlock);
//             }
//
//             for (BasicBlock blockToFalse : r.blocksToFalse){
//                 BrInst brInst = (BrInst) blockToFalse.getInstructions().get(blockToFalse.getInstructions().size() - 1);
//                 brInst.setFalseBranch(falseBlock);
//             }
//         }else if (stmt.getUType() == 5){
//             // Stmt → 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
//            breakBrInsts.push(new ArrayList<>());
//            continueBrInsts.push(new ArrayList<>());
//
//            BasicBlock forStmt1Block = currBasicBlock;
//            if(stmt.forStmt1 != null){
//                visitForStmt(stmt.forStmt1);
//            }
//
//            currBasicBlock = currFunction.createBasicBlock();
//            currBasicBlock.setLoopNum(isInLoop);
//
//            forStmt1Block.createBrInstWithoutCond(currBasicBlock);
//            BasicBlock loopEntryBlock = currBasicBlock;
//
//            VisitResult condRt = new VisitResult();
//            if(stmt.cond2 != null){
//                condRt = visitCond(stmt.cond2);
//            }
//            BasicBlock stmtBlock = currBasicBlock;
//
//            isInLoop++;
//            visitStmt(stmt.stmt);
//            isInLoop--;
//
//            BasicBlock lastBlockInStmt = currBasicBlock;
//            currBasicBlock = currFunction.createBasicBlock();
//            currBasicBlock.setLoopNum(isInLoop);
//
//            lastBlockInStmt.createBrInstWithoutCond(currBasicBlock);
//            BasicBlock forStmt2Block = currBasicBlock;
//
//            if(stmt.forStmt2 != null){
//                visitForStmt(stmt.forStmt2);
//            }
//            forStmt2Block.createBrInstWithoutCond(loopEntryBlock);
//
//            currBasicBlock = currFunction.createBasicBlock();
//            currBasicBlock.setLoopNum(isInLoop);
//
//            BasicBlock loopExitBlock = currBasicBlock;
//
//            for(BasicBlock blockToTrue : condRt.blocksToTrue){
//                BrInst brInst = (BrInst) blockToTrue.getInstructions().get(blockToTrue.getInstructions().size() - 1);
//                brInst.setTrueBranch(stmtBlock);
//            }
//
//            for(BasicBlock blockToFalse : condRt.blocksToFalse){
//                BrInst brInst = (BrInst) blockToFalse.getInstructions().get(blockToFalse.getInstructions().size() - 1);
//                brInst.setFalseBranch(loopExitBlock);
//            }
//
//            for (BrInst continueBrInst : continueBrInsts.pop()){
//                continueBrInst.setDest(forStmt2Block);
//            }
//
//            for(BrInst breakBrInst : breakBrInsts.pop()){
//                breakBrInst.setDest(loopExitBlock);
//            }
//         }else if (stmt.getUType() == 6){
//             // Stmt -> 'break' ';' Stmt -> 'continue' ';'
//             if(isInLoop == 0){
//                 errorRecorder.addError(ErrorType.BREAK_OR_CONTINUE_ERROR, stmt.tkLineNum);
//                 return;
//             }
//
//             if (stmt.type == TokenType.CONTINUETK){
//                 continueBrInsts.peek().add((BrInst) currBasicBlock.createBrInstWithoutCond(null));
//                 currBasicBlock = currFunction.createBasicBlock();
//                 currBasicBlock.setLoopNum(isInLoop);
//             } else if (stmt.type == TokenType.BREAKTK) {
//                 breakBrInsts.peek().add((BrInst) currBasicBlock.createBrInstWithoutCond(null));
//                 currBasicBlock = currFunction.createBasicBlock();
//                 currBasicBlock.setLoopNum(isInLoop);
//             }else {
//                 assert false;
//             }
//         }else if (stmt.getUType() == 7){
//             // Stmt -> 'return' [Exp] ';'
//             Value expIr = null;
//             if(stmt.exp3 != null){
//                 if(isRetExpNotNeed){
//                     errorRecorder.addError(ErrorType.RETURN_NOT_MATCH, stmt.returnLineNum);
//                     return;
//                 }
//                 var result = visitExp(stmt.exp3);
//                 expIr = result.irValue;
//             }
//             currBasicBlock.createReturnInst(expIr);
//         }else if (stmt.getUType() == 8){
//             // Stmt -> 'printf''('StringConst {','Exp}')'';'
//             List<Value> expValues = new ArrayList<>();
//            for (Exp exp : stmt.exps){
//                expValues.add(visitExp(exp).irValue);
//            }
//
//            // 校验格式化字符串 %d 个数与表达式个数匹配
//            int fCharNum = (stmt.stringConst.length() - String.join("", stmt.stringConst.split("%d")).length()) / 2;
//            if(fCharNum != stmt.exps.size()){
//                errorRecorder.addError(ErrorType.PRINTF_NOT_MATCH, stmt.printfLineNum);
//                return;
//            }
//
//            try{
//                for (int i = 1, j = 0; i < stmt.stringConst.length() - 1; i++){
//                    char ch = stmt.stringConst.charAt(i);
//                    if(ch == '%'){
//                        currBasicBlock.createCallInst(Function.BUILD_IN_PUTINT, List.of(expValues.get(j++)));
//                        i++;
//                    } else if (ch == '\\') {
//                        currBasicBlock.createCallInst(Function.BUILD_IN_PUTCH, List.of(new ImmediateValue('\n')));
//                        i++;
//                    }else {
//                        currBasicBlock.createCallInst(Function.BUILD_IN_PUTCH, List.of(new ImmediateValue(ch)));
//                    }
//                }
//            }catch (IndexOutOfBoundsException e){
//                return;
//            }
//         }
//    }
//
//    // 条件表达式 Cond → LOrExp
//    public VisitResult visitCond(Cond cond){
//        return visitLOrExp(cond.lOrExp);
//    }
//
//    // 语句 ForStmt → LVal '=' Exp { ',' LVal '=' Exp }
//    public void visitForStmt(ForStmt stmt){
//        for(int i = 0; i < stmt.lVals.size(); i++){
//            VisitResult r1 = visitLVal(stmt.lVals.get(i));
//            VisitResult r2 = visitExp(stmt.exps.get(i));
//
//            currBasicBlock.createStoreInst(r2.irValue, r1.irValue);
//
//            Symbol lValSymbol = currentSymbolTable.getSymbol(stmt.lVals.get(i).ident);
//            if(lValSymbol instanceof VarSymbol lValVarSym && lValVarSym.isConst){
//                errorRecorder.addError(ErrorType.CHANGE_CONST_VAL, stmt.lVals.get(i).identLineNum);
//            }
//        }
//    }

//    // 逻辑或表达式 LOrExp → LAndExp | LOrExp '||' LAndExp
//    public VisitResult visitLOrExp(LOrExp lOrExp){
//        if(lOrExp.getUType() == 1){
//            VisitResult visitResult = new VisitResult();
//            VisitResult r = visitLAndExp(lOrExp.lAndExp1);
//            visitResult.expType = r.expType;
//
//            visitResult.blocksToTrue.add(r.andBlocks.get(r.andBlocks.size() - 1));
//            visitResult.nearAndBlocks.addAll(r.andBlocks);
//            visitResult.blocksToFalse.addAll(visitResult.nearAndBlocks);
//            return visitResult;
//        } else if (lOrExp.getUType() == 2) {
//            VisitResult visitResult = new VisitResult();
//
//            VisitResult r1 = visitLOrExp(lOrExp.lOrExp);
//            visitResult.blocksToTrue.addAll(r1.blocksToTrue);
//
//            VisitResult r2 = visitLAndExp(lOrExp.lAndExp2);
//            visitResult.blocksToTrue.add(r2.andBlocks.get(r2.andBlocks.size() - 1));
//
//            BasicBlock firstAndBlock = r2.andBlocks.get(0);
//            for (BasicBlock nearAndBlock : r1.nearAndBlocks){
//                BrInst brInst = (BrInst) nearAndBlock.getInstructions().get(nearAndBlock.getInstructions().size() - 1);
//                brInst.setFalseBranch(firstAndBlock); // 左为假，执行右表达式
//            }
//
//            visitResult.nearAndBlocks.addAll(r2.andBlocks);
//            visitResult.blocksToFalse.addAll(visitResult.nearAndBlocks);
//
//            visitResult.expType = r1.expType;
//
//            return visitResult;
//        }else {
//            return new VisitResult();
//        }
//    }
//
//    // 逻辑与表达式 LAndExp → EqExp | LAndExp '&&' EqExp
//    public VisitResult visitLAndExp(LAndExp lAndExp){
//        if(lAndExp.getUType() == 1){
//            VisitResult visitResult = new VisitResult();
//
//            VisitResult r = visitEqExp(lAndExp.eqExp1);
//            visitResult.expType = r.expType;
//            if(!(r.irValue instanceof ICmpInst)){
//                r.irValue = currBasicBlock.createICmpInst(ICmpInstCond.NE, new ImmediateValue(0), r.irValue);
//            }
//            currBasicBlock.createBrInstWithCond(r.irValue, null, null);
//            visitResult.andBlocks.add(currBasicBlock);
//            currBasicBlock = currFunction.createBasicBlock();
//            currBasicBlock.setLoopNum(isInLoop);
//
//            return visitResult;
//        } else if (lAndExp.getUType() == 2) {
//            VisitResult visitResult = new VisitResult();
//
//            VisitResult r1 = visitLAndExp(lAndExp.lAndExp);
//            BasicBlock lastAndBlock = r1.andBlocks.get(r1.andBlocks.size() - 1);
//            BrInst brInLastAndBlock = (BrInst) lastAndBlock.getInstructions().get(lastAndBlock.getInstructions().size() - 1);
//            brInLastAndBlock.setTrueBranch(currBasicBlock); // 左为真，执行右表达式
//            visitResult.andBlocks.addAll(r1.andBlocks);
//
//            VisitResult r2 = visitEqExp(lAndExp.eqExp2);
//            if(!(r2.irValue instanceof ICmpInst)){
//                r2.irValue = currBasicBlock.createICmpInst(ICmpInstCond.NE, new ImmediateValue(0), r2.irValue);
//            }
//            // 右表达式的分支指令
//            currBasicBlock.createBrInstWithCond(r2.irValue, null, null);
//            visitResult.andBlocks.add(currBasicBlock);
//            currBasicBlock = currFunction.createBasicBlock();
//            currBasicBlock.setLoopNum(isInLoop);
//
//            visitResult.expType = r1.expType;
//            return visitResult;
//        }else {
//            return new VisitResult();
//        }
//    }
//
//    // 相等性表达式 EqExp → RelExp | EqExp ('==' | '!=') RelExp
//    public VisitResult visitEqExp(EqExp eqExp){
//        if(eqExp.getUType() == 1){
//            return visitRelExp(eqExp.relExp1);
//        } else if (eqExp.getUType() == 2) {
//            VisitResult visitResult = new VisitResult();
//            VisitResult r1 = visitEqExp(eqExp.eqExp);
//            VisitResult r2 = visitRelExp(eqExp.relExp2);
//
//            visitResult.expType = r1.expType;
//
//            if(r1.irValue instanceof ICmpInst){
//                r1.irValue = currBasicBlock.createZExtInst(IRType.getInt(), r1.irValue);
//            }
//            if(r2.irValue instanceof ICmpInst){
//                r2.irValue = currBasicBlock.createZExtInst(IRType.getInt(), r2.irValue);
//            }
//
//            ICmpInstCond cond = eqExp.op == TokenType.EQL ? ICmpInstCond.EQ : ICmpInstCond.NE;
//            visitResult.irValue = currBasicBlock.createICmpInst(cond, r1.irValue, r2.irValue);
//            return visitResult;
//        }else {
//            return new VisitResult();
//        }
//    }
//
//    // 关系表达式 RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
//    public VisitResult visitRelExp(RelExp relExp){
//        if(relExp.getUType() == 1){
//            return visitAddExp(relExp.addExp1);
//        }else if (relExp.getUType() == 2) {
//            VisitResult visitResult = new VisitResult();
//            VisitResult r1 = visitRelExp(relExp.relExp);
//            VisitResult r2 = visitAddExp(relExp.addExp2);
//
//            if(r1.irValue instanceof ICmpInst){
//                r1.irValue = currBasicBlock.createZExtInst(IRType.getInt(), r1.irValue);
//            }
//            if(r2.irValue instanceof ICmpInst){
//                r2.irValue = currBasicBlock.createZExtInst(IRType.getInt(), r2.irValue);
//            }
//
//            ICmpInstCond cond = switch (relExp.op){
//                case LSS -> ICmpInstCond.SLT;
//                case GRE -> ICmpInstCond.SGT;
//                case LEQ -> ICmpInstCond.SLE;
//                case GEQ -> ICmpInstCond.SGE;
//                default -> null;
//            };
//            visitResult.irValue = currBasicBlock.createICmpInst(cond, r1.irValue, r2.irValue);
//
//            visitResult.expType = r1.expType;
//            return visitResult;
//        }else {
//            return new VisitResult();
//        }
//    }

//    // 主函数定义 MainFuncDef → 'int' 'main' '(' ')' Block
//    public void visitMainFuncDef(MainFuncDef mainFuncDef){
//        FunctionSymbol functionSymbol = new FunctionSymbol();
//        functionSymbol.retType.type = "int";
//        functionSymbol.ident = "main";
////        currentSymbolTable.insertSymbol(functionSymbol);
//
//        // 进入main函数局部作用域
//        currentSymbolTable = currentSymbolTable.createSubTable();
//
//        isRetExpNotNeed = false;
//
//        ArrayList<IRType> irArgTypes = new ArrayList<>();
//        currFunction = irModule.createFunction(IRType.getInt(), irArgTypes);
//        currFunction.setName("main");
//        functionSymbol.targetValue = currFunction;
//        currBasicBlock = currFunction.createBasicBlock();
//        currBasicBlock.setLoopNum(isInLoop);
//
//        visitBlock(mainFuncDef.block);
//
//        if (mainFuncDef.block.isWithoutReturn()){
//            errorRecorder.addError(ErrorType.RETURN_MISS, mainFuncDef.block.blockRLineNum);
//        }
//
//        currBasicBlock = null;
//        currFunction = null;
//
//        // 退出局部作用域
//        currentSymbolTable = currentSymbolTable.getPreTable();
//    }
}
