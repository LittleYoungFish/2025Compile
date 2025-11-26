package frontend.visitor;

import backend.ir.GlobalValue;
import backend.ir.IRType;
import backend.ir.ImmediateValue;
import backend.ir.Value;
import error.ErrorType;
import frontend.parser.node.declaration.*;
import frontend.parser.node.expression.ConstExp;
import frontend.symtable.symbol.FunctionSymbol;
import frontend.symtable.symbol.VarSymbol;

import java.util.ArrayList;
import java.util.List;

public class DeclVisitor extends SubVisitor{
    public DeclVisitor(Visitor visitor) {
        super(visitor);
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
        if(getCurrentSymbolTable().contains(constDef.ident)){
            getErrorRecorder().addError(ErrorType.NAME_REDEFINE, constDef.identLineNum);
            return;
        }

        // 常量符号创建
        VarSymbol varSymbol = new VarSymbol();
        varSymbol.ident = constDef.ident;
        varSymbol.isConst = true;
        varSymbol.varType.type = "int";

        // 若为数组常量
        for (ConstExp dimension : constDef.dimensions){
            varSymbol.varType.dims.add(getExpVisitor().visitConstExp(dimension).constVal);
        }

        // 常量初始化提取
        VisitResult r = getVarVisitor().visitConstInitVal(constDef.constInitVal);
        varSymbol.values.addAll(r.constInitVals);
        List<Value> initValues = r.irValues; // 初始值的IR表示

        if (isGlobalVar()){
            GlobalValue globalValue = getIrModule().createGlobalValue(IRType.getInt().dims(varSymbol.varType.dims), varSymbol.values);
            globalValue.setName(varSymbol.ident);
            varSymbol.targetValue = globalValue; // 符号表关联IR对象
        }else {
            // 分配局部变量空间
            Value localVar = getCurrFunction().getFirstBasicBlock().createAllocInstAndInsert(IRType.getInt().dims(varSymbol.varType.dims));
            varSymbol.targetValue = localVar;

            if(!varSymbol.isArray()){
                // 非数组常量直接赋值
                getCurrBasicBlock().createStoreInst(initValues.get(0), varSymbol.targetValue);
            }else {
                // array with init values
                // 数组常量逐个元素赋值
                for (int i = 0; i < initValues.size(); i++){
                    // 计算第i个元素的多维索引
                    int[] indexs = new int[varSymbol.varType.dims.size()];
                    int pos = i;
                    for (int j = indexs.length - 1; j >= 0; j--){
                        indexs[j] = pos % varSymbol.varType.dims.get(j);
                        pos /= varSymbol.varType.dims.get(j);
                    }

                    // 生成GEP指令获取数组元素的地址
                    Value initValue = initValues.get(i);
                    Value arrayPtr = getCurrBasicBlock().createGetElementPtrInst(varSymbol.targetValue, List.of(new ImmediateValue(0), new ImmediateValue(0)));
                    for (int j = 0; j < indexs.length; j++){
                        int visitIdx = indexs[j];
                        List<Value> offsets = j == indexs.length - 1 ? List.of(new ImmediateValue(visitIdx)) : List.of(new ImmediateValue(visitIdx), new ImmediateValue(0));
                        arrayPtr = getCurrBasicBlock().createGetElementPtrInst(arrayPtr, offsets);
                    }
                    getCurrBasicBlock().createStoreInst(initValue, arrayPtr);
                }
            }
        }

        getCurrentSymbolTable().insertSymbol(varSymbol);
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
        if (getCurrentSymbolTable().contains(varDef.ident)){
            getErrorRecorder().addError(ErrorType.NAME_REDEFINE, varDef.identLineNum);
            return;
        }

        VarSymbol varSymbol = new VarSymbol();
        varSymbol.isConst = false;
        varSymbol.isStatic = isStatic;
        varSymbol.ident = varDef.ident;
        varSymbol.varType.type = "int";

        for (ConstExp dimension : varDef.dimensions){
            varSymbol.varType.dims.add(getExpVisitor().visitConstExp(dimension).constVal);
        }

        if(isGlobalVar()){
            if (varDef.initVal !=null){
                VisitResult r = getVarVisitor().visitInitVal(varDef.initVal);
                varSymbol.values.addAll(r.constInitVals);
            }
            GlobalValue globalValue = getIrModule().createGlobalValue(IRType.getInt().dims(varSymbol.varType.dims), varSymbol.values);
            globalValue.setName(varSymbol.ident);
            varSymbol.targetValue = globalValue;
        }else {
            // 非全局变量
            if(varSymbol.isStatic){
                // 静态局部变量
                // 1. 生成唯一命名的key（函数名_变量名）
                String funcName = getCurrFunction().getName().substring(1); // 原逻辑：去掉前缀下划线
                String baseKey = funcName + "_" + varSymbol.ident;
                // 2. 获取序号：首次为0（命名为main_a），后续递增（main_a_1/main_a_2）
                int seq = getStaticVarCounter().getOrDefault(baseKey, 0);
                String staticGlobalName = baseKey + (seq == 0 ? "" : "_" + seq); // 核心：动态生成唯一名称
                getStaticVarCounter().put(baseKey, seq + 1); // 更新序号

                // 3. 检查缓存：避免重复创建全局值（解决重定义核心）
                GlobalValue staticGlobal;
                if (getStaticGlobalMap().containsKey(staticGlobalName)) {
                    staticGlobal = getStaticGlobalMap().get(staticGlobalName);
                } else {
                    // 静态局部变量无维度，dims 为空列表
                    List<Integer> initVals = new ArrayList<>();
                    // 若有显式初始化值则用显式值，无则默认 0（这里先加 0，后续初始化逻辑会覆盖）
                    if (varDef.initVal != null) {
                        VisitResult r = getVarVisitor().visitInitVal(varDef.initVal);
                        initVals.addAll(r.constInitVals);
                    } else {
                        initVals.add(0); // 静态变量默认初始化为 0
                    }
                    // 创建全局值（全局数据区存储）
                    staticGlobal = getIrModule().createGlobalValue(IRType.getInt().dims(varSymbol.varType.dims), initVals);
                    staticGlobal.setName(staticGlobalName);
                    getStaticGlobalMap().put(staticGlobalName, staticGlobal);
                }
                varSymbol.targetValue = staticGlobal;

                // 若有显式初始化，生成 store 指令覆盖默认值
                if (varDef.initVal != null) {
                    //VisitResult r = visitInitVal(varDef.initVal);
                    // 静态局部变量初始化：全局值仅需赋值一次，直接生成 store 到全局值地址
                    //currBasicBlock.createStoreInst(r.irValues.get(0), varSymbol.targetValue);
                }
            }else {
                Value localVar = getCurrFunction().getFirstBasicBlock().createAllocInstAndInsert(IRType.getInt().dims(varSymbol.varType.dims));
                varSymbol.targetValue = localVar;
                if (varDef.initVal != null) {
                    VisitResult r = getVarVisitor().visitInitVal(varDef.initVal);
                    List<Value> initValues = r.irValues;
                    if (!varSymbol.isArray()) {
                        getCurrBasicBlock().createStoreInst(r.irValues.get(0), varSymbol.targetValue);
                    } else {
                        for (int i = 0; i < initValues.size(); i++) {
                            int[] indexs = new int[varSymbol.varType.dims.size()];
                            int pos = i;
                            for (int j = indexs.length - 1; j >= 0; j--) {
                                indexs[j] = pos % varSymbol.varType.dims.get(j);
                                pos /= varSymbol.varType.dims.get(j);
                            }

                            Value initValue = initValues.get(i);
                            Value arrayPtr = getCurrBasicBlock().createGetElementPtrInst(varSymbol.targetValue, List.of(new ImmediateValue(0), new ImmediateValue(0)));
                            for (int j = 0; j < indexs.length; j++) {
                                int visitIdx = indexs[j];
                                List<Value> offsets = j == indexs.length - 1 ? List.of(new ImmediateValue(visitIdx)) : List.of(new ImmediateValue(visitIdx), new ImmediateValue(0));
                                arrayPtr = getCurrBasicBlock().createGetElementPtrInst(arrayPtr, offsets);
                            }
                            getCurrBasicBlock().createStoreInst(initValue, arrayPtr);
                        }
                    }
                }
            }
        }

        getCurrentSymbolTable().insertSymbol(varSymbol);
    }

    // 基本类型 BType → 'int'
    public String visitBType(BType bType){
        return "int";
    }

    // 主函数定义 MainFuncDef → 'int' 'main' '(' ')' Block
    public void visitMainFuncDef(MainFuncDef mainFuncDef){
        FunctionSymbol functionSymbol = new FunctionSymbol();
        functionSymbol.retType.type = "int";
        functionSymbol.ident = "main";
//        currentSymbolTable.insertSymbol(functionSymbol);

        // 进入main函数局部作用域
        setCurrentSymbolTable(getCurrentSymbolTable().createSubTable());

        setRetExpNotNeed(false);

        ArrayList<IRType> irArgTypes = new ArrayList<>();
        setCurrFunction(getIrModule().createFunction(IRType.getInt(), irArgTypes));
        getCurrFunction().setName("main");
        functionSymbol.targetValue = getCurrFunction();
        setCurrBasicBlock(getCurrFunction().createBasicBlock());
        getCurrBasicBlock().setLoopNum(isInLoop());

        getStmtVisitor().visitBlock(mainFuncDef.block);

        if (mainFuncDef.block.isWithoutReturn()){
            getErrorRecorder().addError(ErrorType.RETURN_MISS, mainFuncDef.block.blockRLineNum);
        }

        setCurrBasicBlock(null);
        setCurrFunction(null);

        // 退出局部作用域
        setCurrentSymbolTable(getCurrentSymbolTable().getPreTable());
    }
}
