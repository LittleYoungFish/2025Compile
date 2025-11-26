package frontend.visitor;

import backend.ir.FunctionArgument;
import backend.ir.IRType;
import backend.ir.Value;
import error.ErrorType;
import frontend.parser.node.expression.Exp;
import frontend.parser.node.function.*;
import frontend.symtable.symbol.FunctionSymbol;
import frontend.symtable.symbol.Symbol;
import frontend.symtable.symbol.Type;
import frontend.symtable.symbol.VarSymbol;

import java.util.ArrayList;

public class FuncVisitor extends SubVisitor{
    public FuncVisitor(Visitor visitor) {
        super(visitor);
    }

    // 函数定义 FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
    public void visitFuncDef(FuncDef funcDef){
        if (getCurrentSymbolTable().contains(funcDef.ident)){
            getErrorRecorder().addError(ErrorType.NAME_REDEFINE, funcDef.identLineNum);
            return;
        }
        FunctionSymbol functionSymbol = new FunctionSymbol();
        functionSymbol.ident = funcDef.ident;

        Type retType = visitFuncType(funcDef.funcType);
        functionSymbol.retType = retType;

        // 创建函数局部作用域
        setCurrentSymbolTable(getCurrentSymbolTable().createSubTable());

        if (funcDef.funcFParams != null){
            functionSymbol.paramTypeList.addAll(visitFuncFParams(funcDef.funcFParams));
        }

        // 将函数符号插入父作用域
        getCurrentSymbolTable().getPreTable().insertSymbol(functionSymbol);

        // 标记是否需要返回值
        setRetExpNotNeed(functionSymbol.retType.type.equals("void"));

        ArrayList<IRType> irArgTypes = new ArrayList<>();
        for (Type type : functionSymbol.paramTypeList){
            if (type.dims.isEmpty()){
                irArgTypes.add(IRType.getInt());
            }else {
                irArgTypes.add(IRType.getInt().dims(type.dims.subList(1, type.dims.size())).ptr(1));
            }
        }

        setCurrFunction(getIrModule().createFunction(functionSymbol.retType.type.equals("void") ? IRType.getVoid() : IRType.getInt(), irArgTypes));
        getCurrFunction().setName(functionSymbol.ident);
        functionSymbol.targetValue = getCurrFunction();
        setCurrBasicBlock(getCurrFunction().createBasicBlock());
        getCurrBasicBlock().setLoopNum(isInLoop());

        if (funcDef.funcFParams != null){
            for (int i = getCurrFunction().getArguments().size() - 1; i >= 0; i--){
                FunctionArgument currArgVal = getCurrFunction().getArguments().get(i);
                Symbol currParamSym = getCurrentSymbolTable().getSymbol(funcDef.funcFParams.funcFParams.get(i).ident);
                Value currArgPtr = getCurrFunction().getFirstBasicBlock().createAllocInstAndInsert(currArgVal.getType());
                getCurrBasicBlock().createStoreInst(currArgVal, currArgPtr);
                currParamSym.targetValue = currArgPtr;
            }
        }

        getStmtVisitor().visitBlock(funcDef.block);

        if (functionSymbol.retType.type.equals("void") && funcDef.block.isWithoutReturn()){
            getCurrBasicBlock().createReturnInst(null);
        }

        setCurrBasicBlock(null);
        setCurrFunction(null);

        // 检查非 void 函数的返回值是否缺失
        if(!functionSymbol.retType.type.equals("void") && funcDef.block.isWithoutReturn()){
            getErrorRecorder().addError(ErrorType.RETURN_MISS, funcDef.block.blockRLineNum);
        }

        // 恢复父作用域
        setCurrentSymbolTable(getCurrentSymbolTable().getPreTable());
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
        if(getCurrentSymbolTable().contains(fParam.ident)){
            getErrorRecorder().addError(ErrorType.NAME_REDEFINE, fParam.identLineNum);
            return null;
        }

        VarSymbol varSymbol = new VarSymbol();
        varSymbol.ident = fParam.ident;
        varSymbol.isConst = false;
        varSymbol.varType.type = getDeclVisitor().visitBType(fParam.type);

        for (int i = 0; i < fParam.count; i++) {
            varSymbol.varType.dims.add(null);
        }

        getCurrentSymbolTable().insertSymbol(varSymbol);

        Type rt = new Type();
        rt.type = "int";
        rt.dims.addAll(varSymbol.varType.dims);
        return rt;
    }

    // 函数实参表达式 FuncRParams → Exp { ',' Exp }
    public VisitResult visitFuncRParams(FuncRParams funcRParams){
        VisitResult visitResult = new VisitResult();
        for(Exp exp : funcRParams.exps){
            VisitResult r = getExpVisitor().visitExp(exp);
            visitResult.paramTypes.add(r.expType);
            visitResult.irValues.add(r.irValue);
        }
        return visitResult;
    }
}
