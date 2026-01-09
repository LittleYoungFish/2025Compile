package frontend.visitor;

import backend.ir.BasicBlock;
import backend.ir.Function;
import backend.ir.ImmediateValue;
import backend.ir.Value;
import backend.ir.inst.BrInst;
import error.ErrorType;
import frontend.lexer.TokenType;
import frontend.parser.node.expression.Exp;
import frontend.parser.node.statement.*;
import frontend.symtable.symbol.Symbol;
import frontend.symtable.symbol.VarSymbol;

import java.util.ArrayList;
import java.util.List;

public class StmtVisitor extends SubVisitor{
    public StmtVisitor(Visitor visitor) {
        super(visitor);
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
            getDeclVisitor().visitDecl(blockItem.decl);
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
            VisitResult rtLVal = getVarVisitor().visitLVal(stmt.lVal);
            Symbol lValSymbol = getCurrentSymbolTable().getSymbol(stmt.lVal.ident);
            if(lValSymbol instanceof VarSymbol lValVarSym && lValVarSym.isConst){
                getErrorRecorder().addError(ErrorType.CHANGE_CONST_VAL, stmt.lVal.identLineNum);
            }

            VisitResult rtExp = getExpVisitor().visitExp(stmt.exp1);
            if (getCurrBasicBlock() != null && rtLVal.irValue != null && rtExp.irValue != null) {
                getCurrBasicBlock().createStoreInst(rtExp.irValue, rtLVal.irValue);
            }
        }else if (stmt.getUType() == 2){
            // Stmt → [Exp] ';'
            if(stmt.exp2 != null){
                getExpVisitor().visitExp(stmt.exp2);
            }
        }else if (stmt.getUType() == 3){
            // Stmt → Block
            setCurrentSymbolTable(getCurrentSymbolTable().createSubTable());
            visitBlock(stmt.block);
            setCurrentSymbolTable(getCurrentSymbolTable().getPreTable());
        }else if (stmt.getUType() == 4){
            // Stmt → 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
            var r = visitCond(stmt.cond1);

            BasicBlock trueBlock = getCurrBasicBlock();
            visitStmt(stmt.ifStmt);

            BasicBlock lastBlockInTrue = getCurrBasicBlock();
            setCurrBasicBlock(getCurrFunction().createBasicBlock());
            getCurrBasicBlock().setLoopNum(isInLoop());

            BasicBlock falseBlock = getCurrBasicBlock();

            if(stmt.elseStmt != null){
                visitStmt(stmt.elseStmt);
                BasicBlock lastBlockInFalse = getCurrBasicBlock();
                setCurrBasicBlock(getCurrFunction().createBasicBlock());
                getCurrBasicBlock().setLoopNum(isInLoop());

                lastBlockInFalse.createBrInstWithoutCond(getCurrBasicBlock());
            }
            lastBlockInTrue.createBrInstWithoutCond(getCurrBasicBlock());

            for (BasicBlock blockToTrue : r.blocksToTrue){
                if (blockToTrue.getInstructions().isEmpty()) continue;
                BrInst brInst = (BrInst) blockToTrue.getInstructions().get(blockToTrue.getInstructions().size() - 1);
                brInst.setTrueBranch(trueBlock);
            }

            for (BasicBlock blockToFalse : r.blocksToFalse){
                if (blockToFalse.getInstructions().isEmpty()) continue;
                BrInst brInst = (BrInst) blockToFalse.getInstructions().get(blockToFalse.getInstructions().size() - 1);
                brInst.setFalseBranch(falseBlock);
            }
        }else if (stmt.getUType() == 5){
            // Stmt → 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
            getBreakBrInsts().push(new ArrayList<>());
            getContinueBrInsts().push(new ArrayList<>());

            BasicBlock forStmt1Block = getCurrBasicBlock();
            if(stmt.forStmt1 != null){
                visitForStmt(stmt.forStmt1);
            }

            setCurrBasicBlock(getCurrFunction().createBasicBlock());
            getCurrBasicBlock().setLoopNum(isInLoop());

            forStmt1Block.createBrInstWithoutCond(getCurrBasicBlock());
            BasicBlock loopEntryBlock = getCurrBasicBlock();

            VisitResult condRt = new VisitResult();
            if(stmt.cond2 != null){
                condRt = visitCond(stmt.cond2);
            }
            BasicBlock stmtBlock = getCurrBasicBlock();

            incrementIsInLoop();
            visitStmt(stmt.stmt);
            decrementIsInLoop();

            BasicBlock lastBlockInStmt = getCurrBasicBlock();
            setCurrBasicBlock(getCurrFunction().createBasicBlock());
            getCurrBasicBlock().setLoopNum(isInLoop());

            lastBlockInStmt.createBrInstWithoutCond(getCurrBasicBlock());
            BasicBlock forStmt2Block = getCurrBasicBlock();

            if(stmt.forStmt2 != null){
                visitForStmt(stmt.forStmt2);
            }
            forStmt2Block.createBrInstWithoutCond(loopEntryBlock);

            setCurrBasicBlock(getCurrFunction().createBasicBlock());
            getCurrBasicBlock().setLoopNum(isInLoop());

            BasicBlock loopExitBlock = getCurrBasicBlock();

            for(BasicBlock blockToTrue : condRt.blocksToTrue){
                BrInst brInst = (BrInst) blockToTrue.getInstructions().get(blockToTrue.getInstructions().size() - 1);
                brInst.setTrueBranch(stmtBlock);
            }

            for(BasicBlock blockToFalse : condRt.blocksToFalse){
                BrInst brInst = (BrInst) blockToFalse.getInstructions().get(blockToFalse.getInstructions().size() - 1);
                brInst.setFalseBranch(loopExitBlock);
            }

            for (BrInst continueBrInst : getContinueBrInsts().pop()){
                continueBrInst.setDest(forStmt2Block);
            }

            for(BrInst breakBrInst : getBreakBrInsts().pop()){
                breakBrInst.setDest(loopExitBlock);
            }
        }else if (stmt.getUType() == 6){
            // Stmt -> 'break' ';' Stmt -> 'continue' ';'
            if(isInLoop() == 0){
                getErrorRecorder().addError(ErrorType.BREAK_OR_CONTINUE_ERROR, stmt.tkLineNum);
                return;
            }

            if (stmt.type == TokenType.CONTINUETK){
                getContinueBrInsts().peek().add((BrInst) getCurrBasicBlock().createBrInstWithoutCond(null));
                setCurrBasicBlock(getCurrFunction().createBasicBlock());
                getCurrBasicBlock().setLoopNum(isInLoop());
            } else if (stmt.type == TokenType.BREAKTK) {
                getBreakBrInsts().peek().add((BrInst) getCurrBasicBlock().createBrInstWithoutCond(null));
                setCurrBasicBlock(getCurrFunction().createBasicBlock());
                getCurrBasicBlock().setLoopNum(isInLoop());
            }else {
                assert false;
            }
        }else if (stmt.getUType() == 7){
            // Stmt -> 'return' [Exp] ';'
            Value expIr = null;
            if(stmt.exp3 != null){
                if(isRetExpNotNeed()){
                    getErrorRecorder().addError(ErrorType.RETURN_NOT_MATCH, stmt.returnLineNum);
                    return;
                }
                var result = getExpVisitor().visitExp(stmt.exp3);
                expIr = result.irValue;
            }
            if (getCurrBasicBlock() != null) {
                // 如果函数是 int 类型但解析结果 expIr 为空（如由于错误 c），补一个 0 防止崩溃
                if (!isRetExpNotNeed() && expIr == null) {
                    expIr = new ImmediateValue(0);
                }
                getCurrBasicBlock().createReturnInst(expIr);
            }
        }else if (stmt.getUType() == 8){
            // Stmt -> 'printf''('StringConst {','Exp}')'';'
            List<Value> expValues = new ArrayList<>();
            for (Exp exp : stmt.exps){
                Value val = getExpVisitor().visitExp(exp).irValue;
                // 如果表达式报错返回 null，补一个 0
                expValues.add(val != null ? val : new ImmediateValue(0));
            }

            // 校验格式化字符串 %d 个数与表达式个数匹配
            int fCharNum = (stmt.stringConst.length() - String.join("", stmt.stringConst.split("%d")).length()) / 2;
            if(fCharNum != stmt.exps.size()){
                getErrorRecorder().addError(ErrorType.PRINTF_NOT_MATCH, stmt.printfLineNum);
                return;
            }

            if (getCurrBasicBlock() != null) {
                try {
                    for (int i = 1, j = 0; i < stmt.stringConst.length() - 1; i++) {
                        char ch = stmt.stringConst.charAt(i);
                        if (ch == '%') {
                            getCurrBasicBlock().createCallInst(Function.BUILD_IN_PUTINT, List.of(expValues.get(j++)));
                            i++;
                        } else if (ch == '\\') {
                            getCurrBasicBlock().createCallInst(Function.BUILD_IN_PUTCH, List.of(new ImmediateValue('\n')));
                            i++;
                        } else {
                            getCurrBasicBlock().createCallInst(Function.BUILD_IN_PUTCH, List.of(new ImmediateValue(ch)));
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    return;
                }
            }
        }
    }

    // 条件表达式 Cond → LOrExp
    public VisitResult visitCond(Cond cond){
        return getExpVisitor().visitLOrExp(cond.lOrExp);
    }

    // 语句 ForStmt → LVal '=' Exp { ',' LVal '=' Exp }
    public void visitForStmt(ForStmt stmt){
        for(int i = 0; i < stmt.lVals.size(); i++){
            VisitResult r1 = getVarVisitor().visitLVal(stmt.lVals.get(i));
            VisitResult r2 = getExpVisitor().visitExp(stmt.exps.get(i));

            getCurrBasicBlock().createStoreInst(r2.irValue, r1.irValue);

            Symbol lValSymbol = getCurrentSymbolTable().getSymbol(stmt.lVals.get(i).ident);
            if(lValSymbol instanceof VarSymbol lValVarSym && lValVarSym.isConst){
                getErrorRecorder().addError(ErrorType.CHANGE_CONST_VAL, stmt.lVals.get(i).identLineNum);
            }
        }
    }
}
