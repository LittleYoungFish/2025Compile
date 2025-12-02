package midend;

import backend.ir.BasicBlock;
import backend.ir.Function;
import backend.ir.FunctionArgument;
import backend.ir.Value;
import backend.ir.inst.*;

import java.util.*;

public class LiveVariableAnalyze {
    private Map<BasicBlock, List<BasicBlock>> dataFlowDiagram = new HashMap<>();
    private Map<BasicBlock, Set<AllocInst>> defSets = new HashMap<>();
    private Map<BasicBlock, Set<AllocInst>> inSets = new HashMap<>();
    private Map<BasicBlock, Set<AllocInst>> outSets = new HashMap<>();

    private List<BasicBlock> basicBlocks = new ArrayList<>();
    private boolean updateInIterate = true;

    public LiveVariableAnalyze(Function function) {
        basicBlocks.addAll(function.getBasicBlocks());

        buildDataFlowDiagram(function);

        for (BasicBlock block : function.getBasicBlocks()){
            defSets.put(block, getDefSet(block));
            inSets.put(block, getUseSet(block));
            outSets.put(block, new HashSet<>());
        }
    }

    private void buildDataFlowDiagram(Function function) {
        for (BasicBlock block : function.getBasicBlocks()){
            List<BasicBlock> nextBlocks = getNextBlocks(block);
            dataFlowDiagram.put(block, nextBlocks);
        }
    }

    private List<BasicBlock> getNextBlocks(BasicBlock block) {
        List<BasicBlock> nextBlocks = new ArrayList<>();
        List<Instruction> instructions = block.getInstructions();
        Instruction lastInstruction = instructions.get(instructions.size() - 1);

        if (lastInstruction instanceof BrInst brInst){
            if (brInst.getDest() != null){
                nextBlocks.add(brInst.getDest());
            }else {
                nextBlocks.add(brInst.getTrueBranch());
                nextBlocks.add(brInst.getFalseBranch());
            }
        }
        return nextBlocks;
    }

    private Set<AllocInst> getDefSet(BasicBlock block) {
        Set<AllocInst> defSet = new HashSet<>();

        for (Instruction instruction : block.getInstructions()) {
            if (instruction instanceof StoreInst storeInst && !(storeInst.getValue() instanceof FunctionArgument)){
                Value ptr = storeInst.getPtr();
                if (ptr instanceof AllocInst allocInst){
                    defSet.add(allocInst);
                }
            }
        }

        return defSet;
    }

    private Set<AllocInst> getUseSet(BasicBlock block) {
        Set<AllocInst> useSet = new HashSet<>();

        for (Instruction instruction : block.getInstructions()) {
            if (instruction instanceof LoadInst loadInst){
                Value ptr = loadInst.getPtr();
                if (ptr instanceof AllocInst allocInst){
                    useSet.add(allocInst);
                }
            }
        }

        return useSet;
    }

    public void analyze(){
        while (!stop()){
            updateInIterate = false;
            iterate();
        }
    }

    private boolean stop(){
        return !updateInIterate;
    }

    private void iterate(){
        for (int i = basicBlocks.size() - 1; i >= 0; i--){
            BasicBlock block = basicBlocks.get(i);
            updateOutSet(block);
            updateInSet(block);
        }
    }

    private void updateOutSet(BasicBlock block){
        List<BasicBlock> nextBlocks = dataFlowDiagram.get(block);

        Set<AllocInst> outSet = outSets.get(block);

        for (BasicBlock nextBlock : nextBlocks){
            int preSize = outSet.size();
            outSet.addAll(inSets.get(nextBlock));
            int curSize = outSet.size();
            if (curSize != preSize){
                updateInIterate = true;
            }
        }
    }

    private void updateInSet(BasicBlock block){
        Set<AllocInst> inSet = inSets.get(block);
        Set<AllocInst> outSet = outSets.get(block);
        Set<AllocInst> defSet = defSets.get(block);

        Set<AllocInst> outSetCopy = new HashSet<>(outSet);
        outSetCopy.removeAll(defSet);
        Set<AllocInst> outWithoutDef = outSetCopy;

        inSet.addAll(outWithoutDef);
    }

    public Map<BasicBlock, Set<AllocInst>> getInSets(){
        return inSets;
    }

    public Map<BasicBlock, Set<AllocInst>> getOutSets(){
        return outSets;
    }

    public Map<BasicBlock, Set<AllocInst>> getDefSets(){
        return defSets;
    }
}
