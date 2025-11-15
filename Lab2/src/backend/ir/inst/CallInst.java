package backend.ir.inst;

import backend.ir.Function;
import backend.ir.IRTypeEnum;
import backend.ir.Value;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class CallInst extends Instruction{
    private Function function;
    private List<Value> params = new ArrayList<>();

    public CallInst(Function f, List<Value> params) {
        super(f.getRetType().clone(), params.toArray(new Value[0]));
        this.function = f;
        this.params.addAll(params);
    }

    public Function getFunction() {
        return function;
    }

    public List<Value> getParams() {
        return params;
    }

    @Override
    public void dump(PrintStream out) {
        out.print("  ");
        if (function.getRetType().getType() != IRTypeEnum.VOID) {
            out.printf("%s = ", getName());
        }
        out.printf("call %s(", function);

        assert params.size() == function.getArguments().size();
        for (int i = 0; i < params.size(); i++) {
            if (i != 0) {
                out.print(", ");
            }
            var currParam = params.get(i);
            out.printf("%s", currParam.toString());
        }

        out.print(")\n");
    }

    @Override
    public void replaceOperand(int pos, Value newOperand) {
        super.replaceOperand(pos, newOperand);
        params.set(pos, newOperand);
    }
}
