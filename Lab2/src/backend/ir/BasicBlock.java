package backend.ir;

import backend.ir.inst.*;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class BasicBlock extends Value{
    private final List<Instruction> instructions = new ArrayList<>();
    private final Function function;
    private int loopNum = 0;

    public BasicBlock(Function belongFunc){
        super(new BasicIRType(IRTypeEnum.LABEL));
        this.function = belongFunc;
    }

    public int getLoopNum(){
        return loopNum;
    }

    public void setLoopNum(int loopNum){
        this.loopNum = loopNum;
    }

    public Function getFunction(){
        return function;
    }

    public BasicBlock getNextBasicBlock(){
        List<BasicBlock> basicBlocks = function.getBasicBlocks();
        int index = basicBlocks.indexOf(this);
        if(index == -1 || index == basicBlocks.size() - 1){
            return null;
        }else {
            return basicBlocks.get(index + 1);
        }
    }

    private Value insertInstruction(Instruction inst){
        instructions.add(inst);
        inst.setBasicBlock(this);
        return this;
    }

    private Integer getImmediateValue(Value value){
        if(value instanceof ImmediateValue iValue){
            return iValue.getValue();
        }else {
            return null;
        }
    }

    public Value createAddInst(Value leftInst, Value rightInst){
        Integer iLeft = getImmediateValue(leftInst), iRight = getImmediateValue(rightInst);
        if(iLeft != null && iRight != null){
            return new ImmediateValue(iLeft + iRight);
        }
        if(iLeft != null && iLeft == 0){ // 0 + a = a
            return rightInst;
        }
        if(iRight != null && iRight == 0){ // a + 0 = a
            return leftInst;
        }
        return insertInstruction(new BinaryInst(BinaryInstOp.ADD, leftInst, rightInst));
    }

    public Value createSubInst(Value leftInst, Value rightInst){
        Integer iLeft = getImmediateValue(leftInst);
        Integer iRight = getImmediateValue(rightInst);
        if(iLeft != null && iRight != null){
            return new ImmediateValue(iLeft - iRight);
        }
        if(iRight != null && iRight == 0){ // a - 0 = a
            return leftInst;
        }
        if(leftInst == rightInst){
            return new ImmediateValue(0);
        }

        return insertInstruction(new BinaryInst(BinaryInstOp.SUB, leftInst, rightInst));
    }

    public Value createMulInst(Value leftInst, Value rightInst){
        Integer iLeft = getImmediateValue(leftInst);
        Integer iRight = getImmediateValue(rightInst);
        if(iLeft != null && iRight != null){
            return new ImmediateValue(iLeft * iRight);
        }
        if(iLeft != null && iLeft == 0){ // 0 * a = 0
            return new ImmediateValue(0);
        }
        if(iRight != null && iRight == 0){ // a * 0 = 0
            return new ImmediateValue(0);
        }
        if(iLeft != null && iLeft == 1){ // 1 * a = a
            return rightInst;
        }
        if(iRight != null && iRight == 1){ // a * 1 = a
            return leftInst;
        }

        return insertInstruction(new BinaryInst(BinaryInstOp.MUL, leftInst, rightInst));
    }

    public Value createDivInst(Value leftInst, Value rightInst){
        Integer iLeft = getImmediateValue(leftInst);
        Integer iRight = getImmediateValue(rightInst);
        if(iLeft != null && iRight != null){
            return new ImmediateValue(iLeft / iRight);
        }
        if(iRight != null && iRight == 1){ // a / 1 = a
            return leftInst;
        }
        if(iLeft != null && iLeft == 0){ // 0 / a = 0
            return new ImmediateValue(0);
        }
        if(leftInst == rightInst){
            return new ImmediateValue(1);
        }
        return insertInstruction(new BinaryInst(BinaryInstOp.SDIV, leftInst, rightInst));
    }

    public Value createSRemInst(Value leftInst, Value rightInst){
        Integer iLeft = getImmediateValue(leftInst);
        Integer iRight = getImmediateValue(rightInst);
        if(iLeft != null && iRight != null){
            return new ImmediateValue(iLeft % iRight);
        }
        return insertInstruction(new BinaryInst(BinaryInstOp.SREM, leftInst, rightInst));
    }

    public Value createReturnInst(Value value){
        Instruction inst = value == null ? new ReturnInst() : new ReturnInst(value);
        return insertInstruction(inst);
    }

    public Value createLoadInst(Value value){
        return insertInstruction(new LoadInst(value));
    }

    public Value createStoreInst(Value value, Value ptr){
        return insertInstruction(new StoreInst(value, ptr));
    }

    public Value createCallInst(Function function, List<Value> params){
        return insertInstruction(new CallInst(function, params));
    }

    public Value createAllocInst(IRType type){
        return insertInstruction(new AllocInst(type));
    }

    public Value createAllocInstAndInsert(IRType type){
        AllocInst allocInst = new AllocInst(type);
        int insertPos;
        for (insertPos = 0; insertPos < instructions.size() && instructions.get(insertPos) instanceof AllocInst; insertPos++);
        instructions.add(insertPos, allocInst);
        allocInst.setBasicBlock(this);
        return allocInst;
    }

    public Value createICmpInst(ICmpInstCond cond, Value left, Value right){
        return insertInstruction(new ICmpInst(cond, left, right));
    }

    public Value createBrInstWithCond(Value cond, BasicBlock ifTrue, BasicBlock ifFalse){
        return insertInstruction(new BrInst(cond, ifTrue, ifFalse));
    }

    public Value createBrInstWithoutCond(BasicBlock dest) {
        return insertInstruction(new BrInst(dest));
    }

    public Value createGetElementPtrInst(Value elementBase, List<Value> offsets) {
        return insertInstruction(new GetElementPtrInst(elementBase, offsets));
    }

    public Value createZExtInst(IRType dstType, Value value) {
        return insertInstruction(new ZExtInst(dstType, value));
    }

    public List<Instruction> getInstructions(){
        return instructions;
    }

    @Override
    public String getName(){
        return "%b" + super.getName();
    }

    public void dump(PrintStream out){
        out.printf("b%s:\n", super.getName());

        for (Instruction instruction : instructions){
            instruction.dump(out);
        }

        out.print("\n");
    }
}
