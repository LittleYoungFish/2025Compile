package backend.ir.inst;

import backend.ir.ArrayIRType;
import backend.ir.BasicIRType;
import backend.ir.IRType;
import backend.ir.Value;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class GetElementPtrInst extends Instruction{
    private Value elementBase;
    private List<Value> offsets = new ArrayList<Value>();

    public GetElementPtrInst(Value elementBase, List<Value> offsets) {
        super(getGEPInstType(elementBase.getType(), offsets.size()),
                buildOperands(elementBase, offsets).toArray(new Value[0]));
        this.elementBase = elementBase;
        this.offsets.addAll(offsets);
    }

    private static List<Value> buildOperands(Value elementBase, List<Value> offsets) {
        List<Value> rt = new ArrayList<>();
        rt.add(elementBase);
        rt.addAll(offsets);
        return rt;
    }

    private static IRType getGEPInstType(IRType elementBaseType, int offsetCount) {
        if (elementBaseType instanceof ArrayIRType arrayIRType){
            List<Integer> oriArrayDims = arrayIRType.getArrayDims();
            List<Integer> rtDims = oriArrayDims.subList(offsetCount - 1, oriArrayDims.size());
            if(rtDims.isEmpty()){
                return new BasicIRType((arrayIRType.getElementType().getType())).ptr(1);
            }else {
                return new ArrayIRType(arrayIRType.getElementType(), rtDims).ptr(1);
            }
        }else {
            assert offsetCount == 1;
            return elementBaseType.clone();
        }
    }

    public Value getElementBase() {
        return elementBase;
    }

    public List<Value> getOffsets() {
        return offsets;
    }

    @Override
    public void dump(PrintStream out) {
        var dataType = elementBase.getType().clone().ptr(elementBase.getType().getPtrNum()-1);
        out.printf("  %s = getelementptr %s, %s",
                getName(),
                dataType,
                elementBase);

        for (Value offset : offsets) {
            out.printf(", %s", offset.toString());
        }
        out.print("\n");
    }

    @Override
    public void replaceOperand(int pos, Value newOperand) {
        super.replaceOperand(pos, newOperand);
        if (pos == 0) {
            elementBase = newOperand;
        } else {
            offsets.set(pos-1, newOperand);
        }
    }
}
