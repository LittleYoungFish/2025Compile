package backend.ir.inst;

import backend.ir.IRType;
import backend.ir.Value;

import java.io.PrintStream;

public class ICmpInst extends Instruction{
    private ICmpInstCond cond;
    private Value leftValue;
    private Value rightValue;

    public enum LogicOp {NONE, AND, OR}
    private LogicOp logicOp = LogicOp.NONE;

    public void setLogicOp(LogicOp logicOp) {
        this.logicOp = logicOp;
    }

    public LogicOp getLogicOp() {
        return logicOp;
    }

    public ICmpInst(ICmpInstCond cond, Value leftValue, Value rightValue){
        super(IRType.getBool(), leftValue, rightValue);
        this.cond = cond;
        this.leftValue = leftValue;
        this.rightValue = rightValue;
        assert leftValue.getType().equals(rightValue.getType());
    }

    public ICmpInstCond getCond() {
        return cond;
    }

    public Value getLeftValue() {
        return leftValue;
    }

    public Value getRightValue() {
        return rightValue;
    }

    @Override
    public void dump(PrintStream out) {
        var type = leftValue.getType();
        out.printf("  %s = icmp %s %s %s, %s\n",
                getName(),
                cond.name().toLowerCase(),
                type.toString(),
                leftValue.getName(),
                rightValue.getName());
    }

    @Override
    public void replaceOperand(int pos, Value newOperand) {
        super.replaceOperand(pos, newOperand);
        switch (pos) {
            case 0:
                leftValue = newOperand;
                break;
            case 1:
                rightValue = newOperand;
                break;
        }
    }
}
