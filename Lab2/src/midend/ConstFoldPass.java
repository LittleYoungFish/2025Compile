package midend;

import backend.ir.*;
import backend.ir.Module;
import backend.ir.inst.BinaryInst;
import backend.ir.inst.Instruction;

import java.util.List;

public class ConstFoldPass {
    private Module module;
    private boolean improve = false;

    public ConstFoldPass(Module module) {
        this.module = module;
    }

    public boolean isImprove(){
        return improve;
    }

    public Module pass(){
        for (Function function : module.getFunctions()) {
            passFunc(function);
        }

        return module;
    }

    private void passFunc(Function function) {
        for (BasicBlock block : function.getBasicBlocks()) {
            passBlock(block);
        }
    }

    private void passBlock(BasicBlock block) {
        List<Instruction> instructions = block.getInstructions();

        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            if (instruction instanceof BinaryInst binaryInst){
                Value valueToReplace = getValueToReplace(binaryInst);
                if (valueToReplace != null){
                    instruction.replaceAllUseWith(valueToReplace, false);
                    improve = true;
                    i--;
                }
            }
        }
    }

    private static Value getValueToReplace(BinaryInst binaryInst) {
        Value leftValue = binaryInst.getLeftValue();
        Value rightValue = binaryInst.getRightValue();
        Integer iLeft = getImmediateValue(leftValue);
        Integer iRight = getImmediateValue(rightValue);

        Value valueToReplace = null;

        switch (binaryInst.getOp()){
            case ADD:
                if (iLeft != null && iRight != null){
                    valueToReplace = new ImmediateValue(iLeft + iRight);
                }
                if (iLeft != null && iLeft == 0){
                    valueToReplace = rightValue;
                }
                if (iRight != null && iRight == 0){
                    valueToReplace = leftValue;
                }
                break;
            case SUB:
                if (iLeft != null && iRight != null){
                    valueToReplace = new ImmediateValue(iLeft - iRight);
                }
                if (iRight != null && iRight == 0){
                    valueToReplace = leftValue;
                }
                if (leftValue == rightValue){
                    valueToReplace = new ImmediateValue(0);
                }
                break;
            case MUL:
                if (iLeft != null && iRight != null){
                    valueToReplace = new ImmediateValue(iLeft * iRight);
                }
                if(iLeft != null && iLeft == 0){
                    valueToReplace = new ImmediateValue(0);
                }
                if (iRight != null && iRight == 0){
                    valueToReplace = new ImmediateValue(0);
                }
                if (iLeft != null && iLeft == 1){
                    valueToReplace = rightValue;
                }
                if (iRight != null && iRight == 1){
                    valueToReplace = leftValue;
                }
                break;
            case SDIV:
                if (iLeft != null && iRight != null){
                    valueToReplace = new ImmediateValue(iLeft / iRight);
                }
                if (iRight != null && iRight == 1){
                    valueToReplace = leftValue;
                }
                if(iLeft != null && iLeft == 0){
                    valueToReplace = new ImmediateValue(0);
                }
                if(leftValue == rightValue){
                    valueToReplace = new ImmediateValue(1);
                }
                break;
            case SREM:
                if (iLeft != null && iRight != null){
                    valueToReplace = new ImmediateValue(iLeft % iRight);
                }
                break;
        }
        return valueToReplace;
    }

    private static Integer getImmediateValue(Value value){
        if (value instanceof ImmediateValue iValue){
            return iValue.getValue();
        }else {
            return null;
        }
    }
}
