package midend;

import backend.ir.BasicBlock;
import backend.ir.Function;
import backend.ir.ImmediateValue;
import backend.ir.Module;
import backend.ir.inst.BinaryInst;
import backend.ir.inst.BinaryInstOp;
import backend.ir.inst.Instruction;

import java.util.List;

public class PeepHolePass {
    private Module module;

    public PeepHolePass(Module module) {
        this.module = module;
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
                if (binaryInst.getOp() == BinaryInstOp.ADD || binaryInst.getOp() == BinaryInstOp.SUB){
                    if (binaryInst.getOp() != BinaryInstOp.SUB
                            && binaryInst.getLeftValue() instanceof ImmediateValue iLeft
                            && iLeft.getValue() == 0){
                        instruction.replaceAllUseWith((Instruction) binaryInst.getRightValue(), false);
                    } else if (binaryInst.getRightValue() instanceof ImmediateValue iRight
                            && iRight.getValue() == 0) {
                        instruction.replaceAllUseWith((Instruction) binaryInst.getLeftValue(), false);
                    }
                } else if (binaryInst.getOp() == BinaryInstOp.MUL || binaryInst.getOp() == BinaryInstOp.SDIV) {
                    if (binaryInst.getOp() != BinaryInstOp.SDIV
                            && binaryInst.getLeftValue() instanceof ImmediateValue iLeft
                            && iLeft.getValue() == 1){
                        instruction.replaceAllUseWith((Instruction) binaryInst.getRightValue(), false);
                    }else if (binaryInst.getRightValue() instanceof ImmediateValue iRight
                            && iRight.getValue() == 1){
                        instruction.replaceAllUseWith((Instruction) binaryInst.getLeftValue(), false);
                    }
                }
            }
        }
    }
}
