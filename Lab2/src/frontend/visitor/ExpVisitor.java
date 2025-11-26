package frontend.visitor;

import backend.ir.*;
import backend.ir.inst.BrInst;
import backend.ir.inst.ICmpInst;
import backend.ir.inst.ICmpInstCond;
import error.ErrorType;
import frontend.lexer.TokenType;
import frontend.parser.node.expression.*;
import frontend.symtable.symbol.FunctionSymbol;
import frontend.symtable.symbol.Symbol;

import java.util.List;

public class ExpVisitor extends SubVisitor{
    public ExpVisitor(Visitor visitor) {
        super(visitor);
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
            VisitResult r2 = visitMulExp(addExp.mulExp2);
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
            if (getCurrBasicBlock() != null){
                if (addExp.op == TokenType.PLUS){
                    visitResult.irValue = getCurrBasicBlock().createAddInst(r1.irValue, r2.irValue);
                }else {
                    visitResult.irValue = getCurrBasicBlock().createSubInst(r1.irValue, r2.irValue);
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

            if (getCurrBasicBlock() != null){
                if (mulExp.op == TokenType.MULT){
                    visitResult.irValue = getCurrBasicBlock().createMulInst(r1.irValue, r2.irValue);
                } else if (mulExp.op == TokenType.DIV) {
                    visitResult.irValue = getCurrBasicBlock().createDivInst(r1.irValue, r2.irValue);
                } else if (mulExp.op == TokenType.MOD) {
                    visitResult.irValue = getCurrBasicBlock().createSRemInst(r1.irValue, r2.irValue);
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
            Symbol symbol = getCurrentSymbolTable().getSymbol(unaryExp.ident);

            if (symbol == null){
                getErrorRecorder().addError(ErrorType.UNDEFINED_NAME, unaryExp.identLineNum);
                visitResult.expType.type = "int";
                return visitResult;
            }

            FunctionSymbol functionSymbol = (FunctionSymbol) symbol;

            visitResult.expType = functionSymbol.retType;

            if (unaryExp.funcRParams != null){
                VisitResult r = getFuncVisitor().visitFuncRParams(unaryExp.funcRParams);

                // 校验参数个数
                if(unaryExp.funcRParams.exps.size() != functionSymbol.paramTypeList.size()){
                    getErrorRecorder().addError(ErrorType.NUM_OF_PARAM_NOT_MATCH, unaryExp.identLineNum);
                    return visitResult;
                }

                // 校验参数类型
                for (int i = 0; i < functionSymbol.paramTypeList.size(); i++){
                    if (!functionSymbol.paramTypeList.get(i).equals(r.paramTypes.get(i))){
                        getErrorRecorder().addError(ErrorType.TYPE_OF_PARAM_NOT_MATCH, unaryExp.identLineNum);
                    }
                }

                visitResult.irValue = getCurrBasicBlock().createCallInst((Function) functionSymbol.targetValue, r.irValues);
            }else {
                if(!functionSymbol.paramTypeList.isEmpty()){
                    getErrorRecorder().addError(ErrorType.NUM_OF_PARAM_NOT_MATCH, unaryExp.identLineNum);
                    return visitResult;
                }
                visitResult.irValue = getCurrBasicBlock().createCallInst((Function) functionSymbol.targetValue, List.of());
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

            if (getCurrBasicBlock() != null){
                TokenType op = visitUnaryOp(unaryExp.unaryOp);
                if (op == TokenType.PLUS){
                    visitResult.irValue = r.irValue;
                }else if(op == TokenType.MINU){
                    visitResult.irValue = getCurrBasicBlock().createSubInst(new ImmediateValue(0), r.irValue);
                }else {
                    //visitResult.irValue = currBasicBlock.createICmpInst(ICmpInstCond.EQ, new ImmediateValue(0), r.irValue);
                    // 1. 第一步：比较 x == 0 → 得到 i1 类型（true 表示 x=0）
                    Value icmp = getCurrBasicBlock().createICmpInst(ICmpInstCond.EQ, new ImmediateValue(0), r.irValue);
                    // 2. 第二步：将 i1 零扩展为 i32（true→1，false→0），符合 C 语言 !x 的语义
                    Value zext = getCurrBasicBlock().createZExtInst(IRType.getInt(), icmp); // IRType.getInt() 是 i32
                    visitResult.irValue = zext;
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
            VisitResult r = getVarVisitor().visitLVal(primaryExp.lVal);
            if (getCurrBasicBlock() != null){
                if(r.lvalLoadNotNeed){
                    return r;
                }
                r.irValue = getCurrBasicBlock().createLoadInst(r.irValue);
            }
            return r;
        } else if (primaryExp.getUType() == 3) {
            return getVarVisitor().visitNumber(primaryExp.number);
        }else {
            return new VisitResult();
        }
    }

    // 常量表达式 ConstExp → AddExp
    public VisitResult visitConstExp(ConstExp constExp){
        VisitResult visitResult = visitAddExp(constExp.addExp);
        assert visitResult.constVal != null;
        return visitResult;
    }

    // 逻辑或表达式 LOrExp → LAndExp | LOrExp '||' LAndExp
    public VisitResult visitLOrExp(LOrExp lOrExp){
        if(lOrExp.getUType() == 1){
            VisitResult visitResult = new VisitResult();
            VisitResult r = visitLAndExp(lOrExp.lAndExp1);
            visitResult.expType = r.expType;

            visitResult.blocksToTrue.add(r.andBlocks.get(r.andBlocks.size() - 1));
            visitResult.nearAndBlocks.addAll(r.andBlocks);
            visitResult.blocksToFalse.addAll(visitResult.nearAndBlocks);
            return visitResult;
        } else if (lOrExp.getUType() == 2) {
            VisitResult visitResult = new VisitResult();

            VisitResult r1 = visitLOrExp(lOrExp.lOrExp);
            visitResult.blocksToTrue.addAll(r1.blocksToTrue);

            VisitResult r2 = visitLAndExp(lOrExp.lAndExp2);
            visitResult.blocksToTrue.add(r2.andBlocks.get(r2.andBlocks.size() - 1));

            BasicBlock firstAndBlock = r2.andBlocks.get(0);
            for (BasicBlock nearAndBlock : r1.nearAndBlocks){
                BrInst brInst = (BrInst) nearAndBlock.getInstructions().get(nearAndBlock.getInstructions().size() - 1);
                brInst.setFalseBranch(firstAndBlock); // 左为假，执行右表达式
            }

            visitResult.nearAndBlocks.addAll(r2.andBlocks);
            visitResult.blocksToFalse.addAll(visitResult.nearAndBlocks);

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
            if(!(r.irValue instanceof ICmpInst)){
                r.irValue = getCurrBasicBlock().createICmpInst(ICmpInstCond.NE, new ImmediateValue(0), r.irValue);
            }
            getCurrBasicBlock().createBrInstWithCond(r.irValue, null, null);
            visitResult.andBlocks.add(getCurrBasicBlock());
            setCurrBasicBlock(getCurrFunction().createBasicBlock());
            getCurrBasicBlock().setLoopNum(isInLoop());

            return visitResult;
        } else if (lAndExp.getUType() == 2) {
            VisitResult visitResult = new VisitResult();

            VisitResult r1 = visitLAndExp(lAndExp.lAndExp);
            BasicBlock lastAndBlock = r1.andBlocks.get(r1.andBlocks.size() - 1);
            BrInst brInLastAndBlock = (BrInst) lastAndBlock.getInstructions().get(lastAndBlock.getInstructions().size() - 1);
            brInLastAndBlock.setTrueBranch(getCurrBasicBlock()); // 左为真，执行右表达式
            visitResult.andBlocks.addAll(r1.andBlocks);

            VisitResult r2 = visitEqExp(lAndExp.eqExp2);
            if(!(r2.irValue instanceof ICmpInst)){
                r2.irValue = getCurrBasicBlock().createICmpInst(ICmpInstCond.NE, new ImmediateValue(0), r2.irValue);
            }
            // 右表达式的分支指令
            getCurrBasicBlock().createBrInstWithCond(r2.irValue, null, null);
            visitResult.andBlocks.add(getCurrBasicBlock());
            setCurrBasicBlock(getCurrFunction().createBasicBlock());
            getCurrBasicBlock().setLoopNum(isInLoop());

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

            if(r1.irValue instanceof ICmpInst){
                r1.irValue = getCurrBasicBlock().createZExtInst(IRType.getInt(), r1.irValue);
            }
            if(r2.irValue instanceof ICmpInst){
                r2.irValue = getCurrBasicBlock().createZExtInst(IRType.getInt(), r2.irValue);
            }

            ICmpInstCond cond = eqExp.op == TokenType.EQL ? ICmpInstCond.EQ : ICmpInstCond.NE;
            visitResult.irValue = getCurrBasicBlock().createICmpInst(cond, r1.irValue, r2.irValue);
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

            if(r1.irValue instanceof ICmpInst){
                r1.irValue = getCurrBasicBlock().createZExtInst(IRType.getInt(), r1.irValue);
            }
            if(r2.irValue instanceof ICmpInst){
                r2.irValue = getCurrBasicBlock().createZExtInst(IRType.getInt(), r2.irValue);
            }

            ICmpInstCond cond = switch (relExp.op){
                case LSS -> ICmpInstCond.SLT;
                case GRE -> ICmpInstCond.SGT;
                case LEQ -> ICmpInstCond.SLE;
                case GEQ -> ICmpInstCond.SGE;
                default -> null;
            };
            visitResult.irValue = getCurrBasicBlock().createICmpInst(cond, r1.irValue, r2.irValue);

            visitResult.expType = r1.expType;
            return visitResult;
        }else {
            return new VisitResult();
        }
    }
}
