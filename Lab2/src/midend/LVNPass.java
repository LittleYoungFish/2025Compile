package midend;

import backend.ir.*;
import backend.ir.Module;
import backend.ir.inst.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LVNPass {
    private Module module;
    private Map<Integer, Instruction> hashTable = new HashMap<Integer, Instruction>();

    public LVNPass(Module module) {
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
        hashTable.clear();
        HashBuilder hashBuilder = new HashBuilder();

        List<Instruction> instructions = block.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);

            if (instruction instanceof BrInst || instruction instanceof ReturnInst) {
                continue;
            }

            // 函数调用可能会修改全局变量或指针指向的内存，必须清除之前的 Load 记录
            if (instruction instanceof CallInst) {
                hashBuilder.clearAllLoads();
                // Call 指令本身有返回值，所以还要继续执行下面的 hash 逻辑
            }

            if (instruction instanceof StoreInst storeInst){
//                Value ptr = storeInst.getPtr();
//                while (ptr instanceof GetElementPtrInst getElementPtrInst){
//                    ptr = getElementPtrInst.getElementBase();
//                }
//                hashBuilder.removeHash(storeInst.getPtr());
//                continue;
                // Store 改变了内存，之前所有的 Load 结果都不可信了
                // 直接清除所有 Load 相关的 Hash 映射
                hashBuilder.clearAllLoads();
                continue;
            }

            int hash = hashBuilder.hash(instruction);
            if (hashTable.containsKey(hash)){
                instruction.replaceAllUseWith(hashTable.get(hash), false);
                i--;
            }else {
                hashTable.put(hash, instruction);
            }
        }
    }
}

class HashBuilder{
    private Map<String, Integer> descToHash = new HashMap<>();
    private Map<Value, Integer> valueToHash = new HashMap<>();
    // 新增：记录内存地址（ptr）对应的最新值哈希
    private Map<Value, Integer> memoryHash = new HashMap<>();
    private int nextHashValue = 0;

    public String createValueDesc(Value value){
        if (value instanceof GlobalValue globalValue){
            return "g " + globalValue.getName();
        }

        if (value instanceof ImmediateValue immediateValue){
            return "imm " + immediateValue.getValue();
        }

        if (value instanceof AllocInst){
            return "alloca" + value.hashCode();
        }

        if (value instanceof LoadInst loadInst){
            return "load "  + hash(loadInst.getPtr());
        }

        if (value instanceof CallInst){
            return "call" + value.hashCode();
        }

        List<String> hashList = ((User) value).getOperands().stream().map(this::hash).map(Object::toString).toList();
        String operandStr = String.join(", ", hashList);

        if (value instanceof BinaryInst binaryInst){
            return binaryInst.getOp().name() + " " + operandStr;
        }

        if (value instanceof GetElementPtrInst){
            return "gep " + operandStr;
        }

        if (value instanceof ICmpInst iCmpInst){
            return iCmpInst.getCond().name() + " " + operandStr;
        }

        if (value instanceof ZExtInst){
            return "zext " + operandStr;
        }

        return "";
    }

    public int hash(Value value){
        if (valueToHash.containsKey(value)){
            return valueToHash.get(value);
        }
        String desc = createValueDesc(value);
        if (descToHash.containsKey(desc)){
            int hash = descToHash.get(desc);
            valueToHash.put(value, hash);
            return hash;
        }

        int nextHash = nextHashValue++;
        valueToHash.put(value, nextHash);
        descToHash.put(desc, nextHash);

        return nextHash;
    }

    public void removeHash(Value value){
        String desc = createValueDesc(value);
        valueToHash.remove(value);
        descToHash.remove(desc);
    }

    public void clearAllLoads(){
        // 移除所有以 "load " 开头的记录，
        // 意味着下次遇到同样的 load 指令，会重新生成一个新的 hash，而不会复用旧值
        descToHash.entrySet().removeIf(entry -> entry.getKey().startsWith("load "));
    }
}
