package backend.target;

import backend.ir.*;
import backend.ir.Module;
import backend.ir.inst.*;
import backend.target.inst.MipsComment;
import backend.target.inst.MipsInst;
import backend.target.inst.MipsLabel;
import backend.target.inst.MipsText;
import backend.target.value.*;
import frontend.parser.node.variable.InitVal;

import java.util.*;
import java.util.stream.Stream;

public class Translator {
    private Target asmTarget = new Target();
    private ValueManager valueManager = new ValueManager();
    private TempRegisterPool tempRegisterPool = new TempRegisterPool(
            asmTarget,
            Stream.of("t0", "t1", "t2", "t3", "t4", "t5", "t6").map(Register.REGS::get).toList()
    );
    private int memorySizeForLocal = 0;
    private Map<Value, Register> registerTempMap = new HashMap<>();

    public Target getAsmTarget() {
        return asmTarget;
    }

    public void translate(Module irModule){
        for (GlobalValue globalValue : irModule.getGlobalValues()){
            translateGlobalValue(globalValue);
        }

        for (Function func : irModule.getFunctions()){
            translateFunction(func);
        }
    }

    private void translateGlobalValue(GlobalValue globalValue) {
        List<Integer> initVals = globalValue.getInitVals();

        Data dataEntry;
        String varName = globalValue.getName().substring(1);
        if(initVals.isEmpty()){
            if(globalValue.getType() instanceof ArrayIRType arrayIRType){
                // 数组无显式初始化：用 space 指令分配内存，总字节数=4 * 数组总元素数
                int totalElements = arrayIRType.getTotalSize();
                dataEntry = new Data(varName, "space", List.of(4 * totalElements));
            }else {
                // 单个变量无初始化，默认4字节int
                dataEntry = new Data(varName, "word", List.of(0));
            }
        }else {
            if (globalValue.getType() instanceof ArrayIRType arrayIRType) {
                // 数组有显式初始化：补充未初始化元素为 0，确保总元素数正确
                int totalElements = arrayIRType.getTotalSize();
                List<Integer> fullInitVals = new ArrayList<>(initVals);
                // 填充未显式初始化的元素为 0（如 arr1[3] = {1} → 补充 [1,0,0]）
                while (fullInitVals.size() < totalElements) {
                    fullInitVals.add(0);
                }
                dataEntry = new Data(varName, "word", Arrays.asList(fullInitVals.toArray()));
            }else {
                // 单个变量有初始化
                dataEntry = new Data(varName, "word", Arrays.asList(initVals.toArray()));
            }
        }

        asmTarget.addData(dataEntry);
        valueManager.putGlobal(globalValue, dataEntry.getLabel());
    }

    private void translateFunction(Function func) {
        String funcName = func.getName().substring(1);  // 去掉函数名前缀（@getint→getint）

        if (funcName.equals("getint")) {
            asmTarget.addText(new MipsLabel("getint"));
            // 生成 getint 的系统调用实现
            asmTarget.addText(new MipsInst("li", Register.REGS.get("v0"), new Immediate(5)));
            asmTarget.addText(new MipsInst("syscall"));
            asmTarget.addText(new MipsInst("jr", Register.REGS.get("ra")));
            return;
        }
        // 是否为库函数
        if ((func.getBasicBlocks().isEmpty() || func.getFirstBasicBlock() == null) && (!funcName.equals("getint"))){
            // 生成函数标签，返回指令
            asmTarget.addText(new MipsLabel(funcName));
            asmTarget.addText(new MipsInst("jr", Register.REGS.get("ra")));
            return;
        }

        asmTarget.addText(new MipsLabel(funcName));
        int totalMemorySize = valueManager.putLocal(func);
        memorySizeForLocal = totalMemorySize - func.calcParamSpace();

        if (memorySizeForLocal > 0){
            Register sp = Register.REGS.get("sp");
            asmTarget.addText(new MipsInst("addiu", sp, sp, new Immediate(-memorySizeForLocal)));
        }

        Set<Register> tempRegisters = new HashSet<>(
                Stream.of("s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7",
                                "t0", "t1", "t2", "t3", "t4", "t5", "t6")
                        .map(Register.REGS::get)
                        .toList()
        );
        valueManager.getRegistersInUse().forEach(tempRegisters::remove);
        tempRegisterPool = new TempRegisterPool(asmTarget, new ArrayList<>(tempRegisters));

        for (BasicBlock block : func.getBasicBlocks()) {
            tempRegisterPool.reset();
            registerTempMap.clear();
            translateBasicBlock(block);
        }

        valueManager.clearLocals();
    }

     private void translateBasicBlock(BasicBlock block) {
        MipsLabel label = new MipsLabel(buildBlockLabelName(block));
        asmTarget.addText(label);

        for (Instruction instruction : block.getInstructions()) {
            translateInstruction(instruction);
        }
     }

     private void translateInstruction(Instruction instruction) {
        asmTarget.addText(new MipsComment(instruction));
        if(instruction instanceof BinaryInst inst){
            translateBinaryInst(inst);
        } else if (instruction instanceof BrInst inst) {
            translateBrInst(inst);
        } else if (instruction instanceof CallInst inst) {
            translateCallInst(inst);
        }  else if (instruction instanceof ICmpInst inst) {
            translateICmpInst(inst);
        } else if (instruction instanceof LoadInst inst) {
            translateLoadInst(inst);
        } else if (instruction instanceof StoreInst inst) {
            translateStoreInst(inst);
        } else if (instruction instanceof ReturnInst inst) {
            translateReturnInst(inst);
        } else if (instruction instanceof GetElementPtrInst inst) {
            translateGetElementPtrInst(inst);
        } else if (instruction instanceof ZExtInst inst) {
            translateZExtInst(inst);
        } else if (instruction instanceof AllocInst inst) {
            translateAllocInst(inst);
        }
        Register.freeAllTempRegister();
     }

     private void translateBinaryInst(BinaryInst inst) {
        TargetValue leftValue = getTempRegister(inst.getLeftValue());
        TargetValue rightValue = getTempRegister(inst.getRightValue());
        TargetValue targetValue = allocTempRegisterForInst(inst);

        if (leftValue instanceof Immediate){
            asmTarget.addText(new MipsInst("li", targetValue, leftValue));
            leftValue = targetValue;
        }

        if (rightValue instanceof Immediate){
            Register tmpReg = Register.allocTempRegister();
            asmTarget.addText(new MipsInst("li", tmpReg, rightValue));
            rightValue = tmpReg;
        }

        switch (inst.getOp()){
            case ADD:
            case SUB:
                asmTarget.addText(new MipsInst(inst.getOp().name().toLowerCase() + "u", targetValue, leftValue, rightValue));
                break;
            case MUL:
                asmTarget.addText(new MipsInst(inst.getOp().name().toLowerCase(), targetValue, leftValue, rightValue));
                break;
            case SDIV:
                asmTarget.addText(new MipsInst("div", leftValue, rightValue));
                asmTarget.addText(new MipsInst("mflo", targetValue));
                break;
            case SREM:
                asmTarget.addText(new MipsInst("div", leftValue, rightValue));
                asmTarget.addText(new MipsInst("mfhi", targetValue));
                break;
        }
     }

     private void translateCallInst(CallInst inst) {
        Function func = inst.getFunction();
        if (func == Function.BUILD_IN_PUTINT || func == Function.BUILD_IN_PUTCH){
            asmTarget.addText(new MipsInst("li", Register.REGS.get("v0"), new Immediate(func == Function.BUILD_IN_PUTINT ? 1 : 11)));

            Value inputVal = inst.getParams().get(0);
            TargetValue inputTargetValue = getTempRegister(inputVal);

             Register a0 = Register.REGS.get("a0");
             Register t7 = Register.REGS.get("t7");
             asmTarget.addText(new MipsInst("move", t7, a0));

             if (inputTargetValue instanceof Immediate){
                 asmTarget.addText(new MipsInst("li", a0, inputTargetValue));
             }else {
                 asmTarget.addText(new MipsInst("move", a0, inputTargetValue));
             }

             asmTarget.addText(new MipsInst("syscall"));
             asmTarget.addText(new MipsInst("move", a0, t7));
        } else if (func == Function.BUILD_IN_GETINT) {
            asmTarget.addText(new MipsInst("li", Register.REGS.get("v0"), new Immediate(5)));
            asmTarget.addText(new MipsInst("syscall"));

            TargetValue target = allocTempRegisterForInst(inst);

            asmTarget.addText(new MipsInst("move", target, Register.REGS.get("v0")));
        }else {
            translateCommonFuncCall(inst);
        }
     }

     private void translateBrInst(BrInst inst) {
        BasicBlock nextBlock = inst.getBasicBlock().getNextBasicBlock();
        if(inst.getCond() != null){
            TargetValue cond = getTempRegister(inst.getCond());
            Register registerCond = convertToRegister(cond);
            BasicBlock falseBranch = inst.getFalseBranch();
            String falseBranchName = buildBlockLabelName(falseBranch);
            BasicBlock trueBranch = inst.getTrueBranch();
            String trueBranchName = buildBlockLabelName(trueBranch);

            if(nextBlock != falseBranch){
                asmTarget.addText(new MipsInst("beqz", registerCond, new Label(falseBranchName)));
            }
            if(nextBlock != trueBranch){
                asmTarget.addText(new MipsInst("bnez", registerCond, new Label(trueBranchName)));
            }
        }else {
            BasicBlock dest = inst.getDest();
            String destBranchName = buildBlockLabelName(dest);

            if(nextBlock != dest){
                asmTarget.addText(new MipsInst("j", new Label(destBranchName)));
            }
        }
     }


     private void translateICmpInst(ICmpInst inst) {
         TargetValue leftValue = getTempRegister(inst.getLeftValue());
         TargetValue rightValue = getTempRegister(inst.getRightValue());
         TargetValue targetValue = allocTempRegisterForInst(inst);

         if (leftValue instanceof Immediate){
             asmTarget.addText(new MipsInst("li", targetValue, leftValue));
             leftValue = targetValue;
         }

         if (rightValue instanceof Immediate){
             Register register = Register.allocTempRegister();
             asmTarget.addText(new MipsInst("li", register, rightValue));
             rightValue = register;
         }

         String instName = switch (inst.getCond()) {
             case EQ -> "seq";
             case NE -> "sne";
             case SGE -> "sge";
             case SGT -> "sgt";
             case SLE -> "sle";
             case SLT -> "slt";
         };

         asmTarget.addText(new MipsInst(instName, targetValue, leftValue, rightValue));
     }

     private void translateLoadInst(LoadInst inst) {
        if(inst.getPtr() instanceof GetElementPtrInst){
            TargetValue ptr = getTempRegister(inst.getPtr());
            Register registerPtr = convertToRegister(ptr);
            TargetValue target = allocTempRegisterForInst(inst);

            asmTarget.addText(new MipsInst("lw", target, new Offset(registerPtr, 0)));
        }else {
            TargetValue ptr = valueManager.getTargetValue(inst.getPtr());

            if (ptr instanceof Register regPtr){
                registerTempMap.put(inst, regPtr);
                return;
            }

            TargetValue target = allocTempRegisterForInst(inst);

            if (ptr instanceof Offset || ptr instanceof Label){
                asmTarget.addText(new MipsInst("lw", target, ptr));
            }else {
                throw new RuntimeException();
            }
        }
     }

     private void translateStoreInst(StoreInst inst) {
        if(inst.getValue() instanceof FunctionArgument){
            return;
        }

        if (inst.getPtr() instanceof GetElementPtrInst){
            TargetValue ptr = getTempRegister(inst.getPtr());
            TargetValue value = getTempRegister(inst.getValue());
            Register registerValue = convertToRegister(value);
            Register registerPtr = (Register) ptr;

            asmTarget.addText(new MipsInst("sw", registerValue, new Offset(registerPtr, 0)));
        }else {
            TargetValue ptr = valueManager.getTargetValue(inst.getPtr());
            TargetValue value = getTempRegister(inst.getValue());
            Register registerValue = convertToRegister(value);

            if(ptr instanceof Offset || ptr instanceof Label){
                asmTarget.addText(new MipsInst("sw", registerValue, ptr));
            } else if (ptr instanceof Register) {
                asmTarget.addText(new MipsInst("move", ptr, registerValue));
            }else {
                throw new RuntimeException();
            }
        }
     }

     private void translateReturnInst(ReturnInst inst) {
        if(inst.getValue() != null){
            TargetValue value = getTempRegister(inst.getValue());
            Register v0 = Register.REGS.get("v0");

            if(value instanceof Immediate){
                asmTarget.addText(new MipsInst("li", v0, value));
            }else {
                asmTarget.addText(new MipsInst("move", v0, value));
            }
        }

        if (memorySizeForLocal > 0){
            Register sp = Register.REGS.get("sp");
            asmTarget.addText(new MipsInst("addiu", sp, sp, new Immediate(memorySizeForLocal)));
        }

        asmTarget.addText(new MipsInst("jr", Register.REGS.get("ra")));
     }

     private void translateGetElementPtrInst(GetElementPtrInst inst) {
         Value base = inst.getElementBase();
         List<Value> offsets = inst.getOffsets();
         List<Integer> dims = base.getType().getArrayDims();

         TargetValue target = allocTempRegisterForInst(inst);
         Register registerBase = (Register) target;

         if(base instanceof LoadInst || base instanceof GetElementPtrInst){
             TargetValue baseVal = getTempRegister(base);
             asmTarget.addText(new MipsInst("move", registerBase, baseVal));
         }else {
             asmTarget.addText(new MipsInst("la", registerBase, valueManager.getTargetValue(base)));
         }

         Register registerTemp = Register.allocTempRegister();
         int currDim = 0;
         for (Value offset : offsets){
             if (offset instanceof ImmediateValue immediateValue && immediateValue.getValue() == 0){
                 currDim++;
                 continue;
             }
             int memSize = 4;
             for (int i = currDim; i < dims.size(); i++){
                 memSize *= dims.get(i);
             }

             TargetValue offsetVal = getTempRegister(offset);

             if (offsetVal instanceof Immediate){
                 asmTarget.addText(new MipsInst("li", registerTemp, offsetVal));
                 asmTarget.addText(new MipsInst("mul", registerTemp, registerTemp, new Immediate(memSize)));
             }else {
                 asmTarget.addText(new MipsInst("mul", registerTemp, offsetVal, new Immediate(memSize)));
             }

             asmTarget.addText(new MipsInst("addu", registerBase, registerBase, registerTemp));
             currDim++;
         }
     }

     private void translateZExtInst(ZExtInst inst) {
         TargetValue value = getTempRegister(inst.getValue());
         TargetValue target = allocTempRegisterForInst(inst);

         if (value instanceof Immediate){
             asmTarget.addText(new MipsInst("li", target, value));
         }else {
             asmTarget.addText(new MipsInst("move", target, value));
         }
     }

     private void translateAllocInst(AllocInst inst) {
        TargetValue targetValue = valueManager.getTargetValue(inst);
        asmTarget.addText(new MipsComment(inst.getName() + ": " + targetValue));
     }

     private void translateCommonFuncCall(CallInst inst) {
        List<Register> registerToReserve = new ArrayList<>();
        registerToReserve.addAll(Stream.of("ra").map(Register.REGS::get).toList());
        registerToReserve.addAll(valueManager.getRegistersInUse());

        Function func = inst.getFunction();
        Register sp = Register.REGS.get("sp");

        int registerByteSize = 4 * registerToReserve.size();
        int paramByteSize = func.calcParamSpace();
        int allocByteSize = registerByteSize + paramByteSize;

        tempRegisterPool.writeBackToMemoryForAll();

        asmTarget.addText(new MipsInst("addiu", sp, sp, new Immediate(-allocByteSize)));

        reserveRegistersInFuncCall(registerToReserve, paramByteSize);

        if (paramByteSize > 0){
            int base = 0;

            for (int paramCount = 0; paramCount < inst.getParams().size(); paramCount++, base += 4){
                Value param = inst.getParams().get(paramCount);
                TargetValue targetParam = valueManager.getTargetValue(param);

                if (registerTempMap.containsKey(param)){
                    Register reg = registerTempMap.get(param);
                    if(!registerToReserve.contains(reg)){
                        throw new RuntimeException();
                    }
                    int savedRegisterOffset = registerToReserve.indexOf(reg) * 4 + paramByteSize;
                    targetParam = new Offset(sp, savedRegisterOffset);
                }else {
                    if(targetParam instanceof Offset offsetParam){
                        if(tempRegisterPool.getRegister(offsetParam) != null){
                            targetParam = tempRegisterPool.getRegister(offsetParam);
                        }else {
                            targetParam = new Offset(offsetParam.getBase(), offsetParam.getOffset() + allocByteSize);
                        }
                    }
                }

                if (paramCount < 4){
                    Register argReg = Register.REGS.get("a" + paramCount);
                    assignToRegister(argReg, targetParam);
                }else {
                    var registerParam = convertToRegister(targetParam);
                    asmTarget.addText(new MipsInst("sw", registerParam, new Offset(sp, base)));
                    Register.freeAllTempRegister();
                }
            }
        }

        tempRegisterPool.reset();

        asmTarget.addText(new MipsInst("jal", new Label(func.getName().substring(1))));

         recoverRegistersInFuncCall(registerToReserve, paramByteSize);

         asmTarget.addText(new MipsInst("addiu", sp, sp, new Immediate(allocByteSize)));

         if (func.getRetType().getType() != IRTypeEnum.VOID){
             TargetValue target = allocTempRegisterForInst(inst);
             asmTarget.addText(new MipsInst("move", target, Register.REGS.get("v0")));
         }
     }

    private void assignToRegister(Register reg, TargetValue value) {
        if (value instanceof Immediate) {
            asmTarget.addText(new MipsInst("li", reg, value));
        } else if (value instanceof Offset) {
            asmTarget.addText(new MipsInst("lw", reg, value));
        } else if (value instanceof Register) {
            asmTarget.addText(new MipsInst("move", reg, value));
        } else {
            throw new RuntimeException();
        }
    }

    private void reserveRegistersInFuncCall(List<Register> registersToReserve, int baseOffset) {
        var sp = Register.REGS.get("sp");
        int offset = 0;
        for (Register register : registersToReserve) {
            asmTarget.addText(new MipsInst("sw", register, new Offset(sp, baseOffset + offset)));
            offset += 4;
        }
    }

    private void recoverRegistersInFuncCall(List<Register> registersToRecover, int baseOffset) {
        var sp = Register.REGS.get("sp");
        int offset = 0;
        for (Register register : registersToRecover) {
            asmTarget.addText(new MipsInst("lw", register, new Offset(sp, baseOffset + offset)));
            offset += 4;
        }
    }

    private TargetValue getTempRegister(Value value){
        if (registerTempMap.containsKey(value)){
            return registerTempMap.get(value);
        }

        TargetValue targetValue = valueManager.getTargetValue(value);
        if(targetValue instanceof Offset offsetValue){
            return tempRegisterPool.allocTempRegister(offsetValue, false);
        }else {
            return targetValue;
        }
    }

    private static String buildBlockLabelName(BasicBlock block) {
        return block.getFunction().getName().substring(1) + "." + block.getName().substring(1);
    }

    private TargetValue allocTempRegisterForInst(Value inst){
        if (registerTempMap.containsKey(inst)){
            return registerTempMap.get(inst);
        }

        TargetValue instValue = valueManager.getTargetValue(inst);
        if(instValue instanceof Offset offsetValue){
            return tempRegisterPool.allocTempRegister(offsetValue, true);
        }else {
            return instValue;
        }
    }

    private Register convertToRegister(TargetValue targetValue) {
        if (isAddress(targetValue)) {
            var newReg = Register.allocTempRegister();
            asmTarget.addText(new MipsInst("lw", newReg, targetValue));
            return newReg;
        } else if (isImmediate(targetValue)) {
            var newReg = Register.allocTempRegister();
            asmTarget.addText(new MipsInst("li", newReg, targetValue));
            return newReg;
        } else if (targetValue instanceof Register register) {
            return register;
        } else {
            throw new RuntimeException();
        }
    }

    private boolean isImmediate(TargetValue value) {
        return value instanceof Immediate;
    }

    private boolean isAddress(TargetValue value) {
        return value instanceof Label || value instanceof Offset;
    }
}
