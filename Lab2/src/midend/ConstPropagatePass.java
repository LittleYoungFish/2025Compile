package midend;

import backend.ir.*;
import backend.ir.Module;
import backend.ir.inst.AllocInst;
import backend.ir.inst.Instruction;
import backend.ir.inst.LoadInst;
import backend.ir.inst.StoreInst;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConstPropagatePass {
    private Module module;
    private Map<Value, ImmediateValue> immediateMap = new HashMap<>();
    private boolean improve = false;

    public ConstPropagatePass(Module module){
        this.module = module;
    }

    public boolean isImprove(){
        return improve;
    }

    public Module pass(){
        for (Function function : module.getFunctions()){
            passFunc(function);
        }
        return module;
    }

    private void passFunc(Function function){
        for (BasicBlock block : function.getBasicBlocks()){
            passBlock(block);
        }
    }

    private void passBlock(BasicBlock block){
        immediateMap.clear();

        List<Instruction> instructions = block.getInstructions();

        for (int i = 0; i < instructions.size(); i++){
            Instruction instruction = instructions.get(i);
            if (instruction instanceof StoreInst storeInst){
                if (storeInst.getValue() instanceof ImmediateValue immediateValue
                    && storeInst.getPtr() instanceof AllocInst allocInst)
                {
                    immediateMap.put(allocInst, immediateValue);
                }else {
                    immediateMap.remove(storeInst.getPtr());
                }
            } else if (instruction instanceof LoadInst loadInst) {
                if (immediateMap.containsKey(loadInst.getPtr())){
                    instruction.replaceAllUseWith(immediateMap.get(loadInst.getPtr()), false);
                    improve = true;
                    i--;
                }
            }
        }
    }
}
