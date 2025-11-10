package frontend.symtable.symbol;

import java.util.ArrayList;
import java.util.List;

public class FunctionSymbol extends Symbol{
    public Type retType = new Type();
    public final List<Type> paramTypeList = new ArrayList<>();

    // VoidFunc | IntFunc
    @Override
    public String getTypeName() {
        String retBaseType = retType.type.substring(0, 1).toUpperCase() + retType.type.substring(1);
        return retBaseType + "Func";
    }
}
