package midend;

import backend.ir.BasicBlock;
import backend.ir.Function;
import backend.ir.Module;
import backend.ir.Value;
import backend.ir.inst.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeadCodePass {
    private Module module;

    public DeadCodePass(Module module) {
        this.module = module;
    }

    public Module pass(){
        for (Function function : module.getFunctions()) {
            passFunc(function);
        }
        return module;
    }

    private void passFunc(Function function) {
        Set<Instruction> initUsefulSet = new HashSet<>();

        for (BasicBlock block : function.getBasicBlocks()) {
            for (Instruction instruction : block.getInstructions()) {
                if (instruction instanceof ReturnInst
                    || instruction instanceof BrInst
                    || instruction instanceof CallInst
                    || instruction instanceof StoreInst
                ){
                    initUsefulSet.add(instruction);
                }
            }
        }

        Set<Instruction> usefulSet = getUsefulClosure(initUsefulSet);
        for (BasicBlock block : function.getBasicBlocks()) {
            List<Instruction> instructions = block.getInstructions();
            for (int i = 0; i < instructions.size(); i++) {
                Instruction instruction = instructions.get(i);
                if (!usefulSet.contains(instruction)
                    && !(instruction instanceof AllocInst)
                ) {
                    instruction.replaceAllUseWith(null, false);
                    i--;
                }
            }
        }
    }

    private Set<Instruction> getUsefulClosure(Set<Instruction> initUsefulSet) {
        Set<Instruction> usefulSet = new HashSet<>();
        for (Instruction instruction : initUsefulSet) {
            usefulSet.addAll(getUsefulClosure(instruction));
        }
        return usefulSet;
    }

    private Set<Instruction> getUsefulClosure(Instruction instruction) {
        Set<Instruction> usefulSet = new HashSet<>();
        usefulSet.add(instruction);
        for (Value operand : instruction.getOperands()){
            if (operand instanceof Instruction instOperand){
                usefulSet.addAll(getUsefulClosure(instOperand));
            }
        }
        return usefulSet;
    }
}
