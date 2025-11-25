package backend.target;

import backend.ir.*;
import backend.ir.inst.*;
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
}
