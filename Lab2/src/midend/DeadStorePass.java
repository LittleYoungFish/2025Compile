package midend;

import backend.ir.*;
import backend.ir.Module;
import backend.ir.inst.AllocInst;
import backend.ir.inst.Instruction;
import backend.ir.inst.LoadInst;
import backend.ir.inst.StoreInst;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeadStorePass {
    private Module module;
    private Map<BasicBlock, Set<AllocInst>> outSets;
    private Set<AllocInst> varAllocInstSet;

    public DeadStorePass(Module module) {
        this.module = module;
    }

    public Module pass(){
        for (Function f : module.getFunctions()) {
            passFunc(f);
        }
        return module;
    }

    private void passFunc(Function f) {
        if(f.isLibrary()){
            return;
        }
        LiveVariableAnalyze analyze = new LiveVariableAnalyze(f);
        analyze.analyze();
        outSets = analyze.getOutSets();

        varAllocInstSet = new HashSet<>(f.getFirstBasicBlock().getInstructions()
                .stream()
                .filter(inst -> inst instanceof AllocInst)
                .map(inst -> (AllocInst) inst)
                .filter(allocInst -> {
                    IRType dataType = allocInst.getDataType();
                    return dataType.getPtrNum() == 0 && dataType.getArrayDims().isEmpty();
                })
                .toList());

        for (BasicBlock block : f.getBasicBlocks()) {
            passBlock(block);
        }
    }

    private void passBlock(BasicBlock block) {
        Set<AllocInst> outSet = outSets.get(block);
        Set<AllocInst> needStore = new HashSet<>(outSet);

        List<Instruction> instructions = block.getInstructions();

        for (int i = instructions.size() - 1; i >= 0; i--) {
            Instruction inst = instructions.get(i);

            if (inst instanceof LoadInst loadInst){
                Value ptr = loadInst.getPtr();
                if (ptr instanceof AllocInst allocInst && varAllocInstSet.contains(allocInst)){
                    needStore.add(allocInst);
                }
            }else if (inst instanceof StoreInst storeInst){
                Value ptr = storeInst.getPtr();
                if (ptr instanceof AllocInst allocInst && varAllocInstSet.contains(allocInst)){
                    if (needStore.contains(allocInst)){
                        needStore.remove(allocInst);
                    }else {
                        inst.replaceAllUseWith(null, false);
                    }
                }
            }
        }
    }
}
