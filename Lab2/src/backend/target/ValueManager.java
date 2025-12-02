package backend.target;

import backend.ir.*;
import backend.ir.inst.*;
import backend.optimize.ConflictGraph;
import backend.optimize.ConflictGraphBuilder;
import midend.LiveVariableAnalyze;
import backend.target.value.*;

import java.util.*;

public class ValueManager {
    private Map<Value, TargetValue> globalValueMap = new HashMap<>();
    private Map<Value, TargetValue> localValueMap = new HashMap<>();

    public TargetValue getTargetValue(Value value) {
        if (value instanceof ImmediateValue immediateValue){
            return new Immediate(immediateValue.getValue());
        }
        return globalValueMap.getOrDefault(value, localValueMap.getOrDefault(value, null));
    }

    public void putGlobal(Value value, Label label){
        globalValueMap.put(value, label);
    }

    public int putLocal(Function func){
        return graphColorManage(func);
    }

    private int basicManage(Function func) {
        var registersName = List.of("s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7");
        return manageMemory(func, registersName.stream().map(Register.REGS::get).toList(), this::refCountGlobalRegisterManage);
    }

    private int manageMemory(Function func, List<Register> registersToAlloc, GlobalRegisterManager globalRegisterManager){
        // 库函数（无基本块），直接返回0，无需内存管理
        if (func.getBasicBlocks().isEmpty() || func.getFirstBasicBlock() == null) {
            return 0;
        }

        int argNum = func.getArguments().size();
        List<AllocInst> allAllocInsts = new ArrayList<>(
                func.getFirstBasicBlock().getInstructions()
                        .stream()
                        .filter(inst -> inst instanceof AllocInst)
                        .map(inst -> (AllocInst) inst)
                        .toList());

        List<AllocInst> argAllocInsts = allAllocInsts.subList(0, argNum);
        Collections.reverse(argAllocInsts);
        List<AllocInst> varAllocInsts = allAllocInsts.subList(argNum, allAllocInsts.size());

        for (int i = 0; i < argAllocInsts.size() && i < 4; i++){
            AllocInst inst = argAllocInsts.get(i);
            localValueMap.put(inst, Register.REGS.get("a" + i));
        }

        globalRegisterManager.manageGlobalRegister(registersToAlloc, varAllocInsts, func);

        int numOfArgOnRegister = Math.min(argNum, 4);
        int memoryRequired = numOfArgOnRegister * 4;

        for (BasicBlock basicBlock : func.getBasicBlocks()){
            for (Instruction inst : basicBlock.getInstructions()){
                if ((inst instanceof StoreInst)
                        || (inst instanceof BrInst)
                        || (inst instanceof ReturnInst)
                        || (inst instanceof CallInst callInst && callInst.getType().getType() == IRTypeEnum.VOID)) {
                    continue;
                }
                if (localValueMap.containsKey(inst)){
                    continue;
                }

                if (inst instanceof AllocInst allocaInst
                        && allocaInst.getDataType() instanceof ArrayIRType arrayIRType
                        && arrayIRType.getPtrNum() == 0) {
                    memoryRequired += 4 * arrayIRType.getTotalSize();
                } else {
                    memoryRequired += 4;
                }
            }
        }

        int baseOffset = memoryRequired;
        Register sp = Register.REGS.get("sp");

        for (int i = argAllocInsts.size() - 1; i >= 4; i--){
            AllocInst inst = argAllocInsts.get(i);
            baseOffset -= 4;
            localValueMap.put(inst, new Offset(sp, baseOffset));
        }

        baseOffset -= numOfArgOnRegister * 4;

        for (BasicBlock block : func.getBasicBlocks()) {
            for (Instruction inst : block.getInstructions()) {
                if ((inst instanceof StoreInst)
                        || (inst instanceof BrInst)
                        || (inst instanceof ReturnInst)
                        || (inst instanceof CallInst callInst && callInst.getType().getType() == IRTypeEnum.VOID)) {
                    continue;
                }
                if (localValueMap.containsKey(inst)) {
                    continue;
                }

                if (inst instanceof AllocInst allocaInst
                        && allocaInst.getDataType() instanceof ArrayIRType arrayIRType
                        && arrayIRType.getPtrNum() == 0) {
                    baseOffset -= 4 * arrayIRType.getTotalSize();
                } else {
                    baseOffset -= 4;
                }

                localValueMap.put(inst, new Offset(sp, baseOffset));
            }
        }

        if (baseOffset != 0){
            throw new RuntimeException();
        }

        return memoryRequired;
    }

    private int graphColorManage(Function func){
        List<String> registersName = List.of("s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7");
        return manageMemory(func, registersName.stream().map(Register.REGS::get).toList(), this::graphColorGlobalRegisterManage);
    }

    private void graphColorGlobalRegisterManage(List<Register> registers, List<AllocInst> varInsts, Function func){
        LiveVariableAnalyze liveVariableAnalyze = new LiveVariableAnalyze(func);
        liveVariableAnalyze.analyze();

        varInsts = varInsts
                .stream()
                .filter(inst -> inst.getDataType().getArrayDims().isEmpty())
                .toList();

        Map<BasicBlock, Set<AllocInst>> inSets = liveVariableAnalyze.getInSets();
        Map<BasicBlock, Set<AllocInst>> outSets = liveVariableAnalyze.getOutSets();
        Map<BasicBlock, Set<AllocInst>> activeSets = new HashMap<>();

        for (BasicBlock block : func.getBasicBlocks()) {
            Set<AllocInst> activeSet = new HashSet<AllocInst>();
            activeSets.put(block, activeSet);
            activeSet.addAll(inSets.get(block));
            activeSet.addAll(outSets.get(block));
        }

        ConflictGraph conflictGraph = new ConflictGraphBuilder(varInsts, liveVariableAnalyze.getDefSets(), activeSets).getGraph();
        ConflictGraph graphForColor = conflictGraph.copy();
        int degreeThreshold = registers.size();
        Stack<AllocInst> nodesToColor = new Stack<>();

        while (!graphForColor.isEmpty()){
            AllocInst candidate = null;
            for (AllocInst node : graphForColor.getNodes()){
                if (graphForColor.getConflict(node).size() >= degreeThreshold){
                    continue;
                }
                candidate = node;
                break;
            }

            if (candidate == null){
                int maxDegree = degreeThreshold - 1;
                for (AllocInst node : graphForColor.getNodes()){
                    int degree = graphForColor.getConflict(node).size();
                    if (graphForColor.getConflict(node).size() <= maxDegree){
                        continue;
                    }
                    maxDegree = degree;
                    candidate = node;
                    break;
                }
            }else {
                nodesToColor.push(candidate);
            }
            graphForColor.removeNode(candidate);
        }

        while (!nodesToColor.isEmpty()){
            AllocInst node = nodesToColor.pop();
            Set<Register> preserveRegs = new HashSet<>();

            for (AllocInst conflictNode : conflictGraph.getConflict(node)){
                if (localValueMap.containsKey(conflictNode) && localValueMap.get(conflictNode) instanceof Register registerInUse){
                    preserveRegs.add(registerInUse);
                }
            }

            for (Register register : registers){
                if (preserveRegs.contains(register)){
                    continue;
                }
                localValueMap.put(node, register);
                break;
            }
        }
    }

    public void clearLocals(){
        localValueMap.clear();
    }

    public List<Register> getRegistersInUse(){
        return localValueMap.values().stream().filter(elm -> elm instanceof Register).map(elm -> (Register)elm).distinct().toList();
    }

    interface GlobalRegisterManager{
        void manageGlobalRegister(List<Register> registersToAlloc, List<AllocInst> varAllocInsts, Function function);
    }

    private void refCountGlobalRegisterManage(List<Register> registers, List<AllocInst> varInsts, Function function){
        Stack<Register> registersToAlloc = new Stack<>();
        registersToAlloc.addAll(registers);

        List<AllocInst> integerVarInsts = varInsts
                .stream()
                .filter(inst -> inst.getDataType().getArrayDims().isEmpty())
                .toList();

        Map<AllocInst, Integer> refCounts = new HashMap<>();

        for (AllocInst inst : integerVarInsts){
            int refCount = 0;
            for (Use use : inst.getUseList()){
                int loopWeight = ((Instruction) use.getUser()).getBasicBlock().getLoopNum();
                if(loopWeight > 0){
                    refCount += 5 * loopWeight;
                }else {
                    refCount += 1;
                }
            }
            refCounts.put(inst, refCount);
        }

        ArrayList<AllocInst> varInstOrderedByRef = new ArrayList<>(refCounts.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList());
        Collections.reverse(varInstOrderedByRef);

        for (AllocInst inst : varInstOrderedByRef){
            if (registersToAlloc.isEmpty()){
                break;
            }
            localValueMap.put(inst, registersToAlloc.pop());
        }
    }

}
