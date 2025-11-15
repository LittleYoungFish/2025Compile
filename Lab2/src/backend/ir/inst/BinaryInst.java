package backend.ir.inst;

import backend.ir.Value;

import java.io.PrintStream;

public class BinaryInst extends Instruction{
    private BinaryInstOp op;
    private Value leftValue;
    private Value rightValue;

    public BinaryInst(BinaryInstOp op, Value leftValue, Value rightValue){
        super(leftValue.getType(), leftValue, rightValue);
        assert leftValue.getType().equals(rightValue.getType());

        this.op = op;
        this.leftValue = leftValue;
        this.rightValue = rightValue;
    }

    public BinaryInstOp getOp(){
        return op;
    }

    public Value getLeftValue(){
        return leftValue;
    }

    public Value getRightValue(){
        return rightValue;
    }

    @Override
    public void dump(PrintStream out) {
        out.printf("  %s = %s %s %s, %s\n",
                getName(),
                op.name().toLowerCase(),
                getType().toString(),
                leftValue.getName(),
                rightValue.getName());
    }

    @Override
    public void replaceOperand(int index, Value newValue) {
        super.replaceOperand(index, newValue);
        switch (index){
            case 0:
                leftValue = newValue;
                break;
            case 1:
                rightValue = newValue;
                break;
        }
    }
}
