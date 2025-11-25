package backend.target;

import backend.target.inst.MipsInst;
import backend.target.value.Offset;
import backend.target.value.Register;

import java.util.*;

public class TempRegisterPool {
    private Target target;
    private List<Register> registers = new ArrayList<Register>();
    private Stack<Register> registerUnused = new Stack<>();
    private Map<Offset, Register> addrRegisterMap = new HashMap<Offset, Register>();
    private Map<Register, Offset> registerAddrMap = new HashMap<>();
    private Queue<Register> timeQueue = new ArrayDeque<>();

    public TempRegisterPool(Target target, List<Register> registers) {
        this.target = target;
        this.registers.addAll(registers);
        this.registerUnused.addAll(registers);
    }

    public boolean hasBeenAllocated(Offset offset) {
        return addrRegisterMap.containsKey(offset);
    }

    public Register getRegister(Offset offset) {
        return addrRegisterMap.get(offset);
    }

    public Register allocTempRegister(Offset offset, boolean firstTime) {
        if (hasBeenAllocated(offset)) {
            Register reg = getRegister(offset);
            timeQueue.remove(reg);
            timeQueue.add(reg);
            return getRegister(offset);
        }

        if (registerUnused.isEmpty()){
            Register reg = timeQueue.poll();
            Offset regOffset = registerAddrMap.get(reg);
            addrRegisterMap.remove(regOffset);
            registerAddrMap.remove(reg);
            target.addText(new MipsInst("sw", reg, regOffset));
            registerUnused.add(reg);
        }

        Register regToAlloc = registerUnused.pop();
        timeQueue.add(regToAlloc);

        if (!firstTime) {
            target.addText(new MipsInst("lw", regToAlloc, offset));
        }

        addrRegisterMap.put(offset, regToAlloc);
        registerAddrMap.put(regToAlloc, offset);

        return regToAlloc;
    }

    public void writeBackToMemoryForAll(){
        while (!timeQueue.isEmpty()){
            Register reg = timeQueue.poll();
            Offset offset = registerAddrMap.get(reg);
            target.addText(new MipsInst("sw", reg, offset));
        }
    }

    public void reset(){
        registerUnused.clear();
        timeQueue.clear();
        addrRegisterMap.clear();
        registerAddrMap.clear();
        registerUnused.addAll(registers);
    }
}
