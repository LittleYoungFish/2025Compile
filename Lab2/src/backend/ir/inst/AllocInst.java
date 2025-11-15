package backend.ir.inst;

import backend.ir.IRType;
import backend.ir.Value;

import java.io.PrintStream;

public class AllocInst extends Instruction{
    public AllocInst(IRType dataType) {
        super(dataType.clone().ptr(dataType.getPtrNum() + 1)); // the type of alloc var is actually the address of the data
    }

    public IRType getDataType() {
        IRType dataType = getType().clone().ptr(getType().getPtrNum() - 1);// remove pointer
        return dataType;
    }

    @Override
    public void dump(PrintStream out){
        IRType dataType = getType().clone().ptr(getType().getPtrNum() - 1); // remove pointer
        out.printf("  %s = alloca %s\n", getName(), dataType);
    }

    @Override
    public void replaceOperand(int pos, Value newOperand){
        super.replaceOperand(pos, newOperand);
    }
}
