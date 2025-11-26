package frontend.visitor;

import backend.ir.ImmediateValue;
import backend.ir.Value;
import backend.ir.inst.AllocInst;
import error.ErrorType;
import frontend.parser.node.expression.ConstExp;
import frontend.parser.node.expression.Exp;
import frontend.parser.node.variable.ConstInitVal;
import frontend.parser.node.variable.InitVal;
import frontend.parser.node.variable.LVal;
import frontend.parser.node.variable.Number;
import frontend.symtable.symbol.Symbol;
import frontend.symtable.symbol.Type;
import frontend.symtable.symbol.VarSymbol;

import java.util.ArrayList;
import java.util.List;

public class VarVisitor extends SubVisitor{

    public VarVisitor(Visitor visitor) {
        super(visitor);
    }

    // 常量初值 ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}'
    public VisitResult visitConstInitVal(ConstInitVal constInitVal){
        if(constInitVal.getUType() == 1){
            //ConstInitVal → ConstExp
            VisitResult visitResult = new VisitResult();
            var r = getExpVisitor().visitConstExp(constInitVal.constExp);
            visitResult.constInitVals.add(r.constVal);
            // 不使用addAll，addAll用于 const int arr[2][2] = {{1,2}, {3,4}}，内层的 {1,2} 和 {3,4} 本身也是 ConstInitVal
            visitResult.irValues.add(r.irValue);
            return visitResult;
        } else if (constInitVal.getUType() == 2) {
            //ConstInitVal → '{' [ ConstExp { ',' ConstExp } ] '}'
            VisitResult visitResult = new VisitResult();
            for (ConstExp constExp : constInitVal.constExps){
                var r = getExpVisitor().visitConstExp(constExp);
                visitResult.constInitVals.add(r.constVal);
                visitResult.irValues.add(r.irValue);
            }
            return visitResult;
        }else {
            return new VisitResult();
        }
    }

    // 变量初值 InitVal → Exp | '{' [ Exp { ',' Exp } ] '}'
    public VisitResult visitInitVal(InitVal initVal){
        if (initVal.getUType() == 1){
            // InitVal → Exp
            VisitResult visitResult = new VisitResult();
            var r = getExpVisitor().visitExp(initVal.exp);
            visitResult.constInitVals.add(r.constVal);
            visitResult.irValues.add(r.irValue);
            return visitResult;
        } else if (initVal.getUType() == 2) {
            // InitVal → '{' [ Exp { ',' Exp } ] '}'
            VisitResult visitResult = new VisitResult();
            for (Exp exp : initVal.exps){
                var r = getExpVisitor().visitExp(exp);
                visitResult.constInitVals.add(r.constVal);
                visitResult.irValues.add(r.irValue);
            }
            return visitResult;
        }else {
            return new VisitResult();
        }
    }

    // 左值表达式 LVal → Ident ['[' Exp ']']
    public VisitResult visitLVal(LVal lVal){
        VisitResult visitResult = new VisitResult();

        Symbol symbol = getCurrentSymbolTable().getSymbol(lVal.ident);

        if (symbol == null){
            getErrorRecorder().addError(ErrorType.UNDEFINED_NAME, lVal.identLineNum);
            visitResult.expType.type = "int";
            return visitResult;
        }

        VarSymbol varSymbol = (VarSymbol) symbol;

        // 数组维度信息收集
        List<Integer> accessDims = new ArrayList<>();
        List<Value> irVisitDims = new ArrayList<>();
        for (Exp exp : lVal.dimensions){
            VisitResult rtExp = getExpVisitor().visitExp(exp);
            accessDims.add(rtExp.constVal);
            irVisitDims.add(rtExp.irValue);
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

        if (getCurrBasicBlock() != null){
            if(varSymbol.isArray()){
                List<Integer> dims = varSymbol.varType.dims;

                Value arrayPtr;
                Value symbolValue = varSymbol.targetValue;

                if(symbolValue instanceof AllocInst allocSymVal && allocSymVal.getDataType().getPtrNum() != 0){
                    arrayPtr = getCurrBasicBlock().createLoadInst(symbolValue);
                }else {
                    arrayPtr = getCurrBasicBlock().createGetElementPtrInst(symbolValue, List.of(new ImmediateValue(0), new ImmediateValue(0)));
                }

                for (int i = 0; i < irVisitDims.size(); i++){
                    Value visitDim = irVisitDims.get(i);
                    List<Value> offsets = (i == dims.size() - 1) ? List.of(visitDim) : List.of(visitDim, new ImmediateValue(0));
                    arrayPtr = getCurrBasicBlock().createGetElementPtrInst(arrayPtr, offsets);
                }

                visitResult.irValue = arrayPtr;
                visitResult.lvalLoadNotNeed = dims.size() != irVisitDims.size();
            }else {
                if(varSymbol.isConst){
                    visitResult.irValue = new ImmediateValue(varSymbol.values.get(0));
                    visitResult.lvalLoadNotNeed = true;
                }else {
                    visitResult.irValue = varSymbol.targetValue;
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
        visitResult.irValue = new ImmediateValue(visitResult.constVal);
        return visitResult;
    }
}
