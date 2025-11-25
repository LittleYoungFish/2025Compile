package backend.ir;

import backend.ir.inst.BrInst;
import backend.ir.inst.ICmpInstCond;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class Module {
    List<GlobalValue> globalValues = new ArrayList<GlobalValue>();
    List<Function> functions = new ArrayList<>();

    public List<GlobalValue> getGlobalValues() {
        return globalValues;
    }

    public List<Function> getFunctions() {
        return functions;
    }

    public GlobalValue createGlobalValue(IRType type, List<Integer> initVals) {
        GlobalValue globalValue = new GlobalValue(type, initVals);
        globalValues.add(globalValue);
        return globalValue;
    }

    public Function createFunction(IRType retType, List<IRType> argTypes) {
        Function function = new Function(retType, argTypes);
        functions.add(function);
        return function;
    }

    public void dump(PrintStream out){
        for(GlobalValue globalValue : globalValues){
            globalValue.dump(out);
        }

        for(Function function : functions){
            if (!function.isLibrary()) {
                function.dump(out);
                NameAllocator.getInstance().reset();
            }
        }
    }
}
