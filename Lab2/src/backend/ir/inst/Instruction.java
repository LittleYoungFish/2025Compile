package backend.ir.inst;

import backend.ir.*;

import java.io.PrintStream;

public abstract class Instruction extends User {
    private BasicBlock basicBlock;

    public Instruction(IRType type, Value... operands){
        super(type, operands);
    }

    public BasicBlock getBasicBlock() {
        return basicBlock;
    }

    public void setBasicBlock(BasicBlock basicBlock) {
        this.basicBlock = basicBlock;
    }

    public void replaceAllUseWith(Value newValue, boolean needInsert){
        int index = basicBlock.getInstructions().indexOf(this);
        if (index < 0){
            throw new RuntimeException();
        }
        if (needInsert){
            if (newValue instanceof Instruction newInst){
                basicBlock.getInstructions().set(index, newInst);
            }else {
                throw new RuntimeException();
            }
        }else {
            basicBlock.getInstructions().remove(index);
        }

        for (Use use : getUseList()){
            use.getUser().replaceOperand(use.getPos(), newValue);
        }
    }

    @Override
    public String getName(){
        return "%t" + super.getName();
    }

    public void dump(PrintStream out){
        out.printf("  %%%s = undefined\n", getName());
    }
}
