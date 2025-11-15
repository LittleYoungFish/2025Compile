package backend.ir;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class GlobalValue extends Value{
    private IRType dataType;
    private List<Integer> initVals = new ArrayList<Integer>();

    public GlobalValue(IRType dataType, List<Integer> initVals){
        super(dataType.clone().ptr(dataType.getPtrNum() + 1)); // the type of global var is actually the address of the data
        this.dataType = dataType;
        this.initVals = initVals;
    }

    public IRType getDataType() {
        return dataType;
    }

    public List<Integer> getInitVals() {
        return initVals;
    }

    @Override
    public String getName(){
        return "@" + super.getName();
    }

    public void dump(PrintStream out){
        out.printf("%s = dso_local global %s\n", getName(), dataType.initValsToString(initVals));
    }
}
