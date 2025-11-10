package backend.ir;

import java.util.ArrayList;
import java.util.List;

public class User extends Value{
    protected final List<Value> operands = new ArrayList<>();

    public User(IRType type, Value... operands){
        super(type);

        int pos = 0;
        for(Value operand : operands){
            if (operand != null){
                operand.addUse(this, pos);
            }
            this.operands.add(operand);
            pos++;
        }
    }

    public List<Value> getOperands(){
        return operands;
    }

    public void replaceOperand(int pos, Value newOperand){
        operands.set(pos, newOperand);
    }
}
