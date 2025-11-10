package frontend.symtable.symbol;

import java.util.ArrayList;
import java.util.List;

public class VarSymbol extends Symbol{
    public Type varType = new Type();
    public boolean isConst = false;
    public boolean isStatic = false; // 是否为静态属性
    public final List<Integer> values = new ArrayList<>();

    public boolean isArray(){
        return !varType.dims.isEmpty();
    }

    // ConstInt | ConstIntArray | StaticInt | Int | IntArray | StaticIntArray
    @Override
    public String getTypeName() {
        String baseType = varType.type.substring(0, 1).toUpperCase() + varType.type.substring(1);
        StringBuilder typeName = new StringBuilder();

        if (isStatic) {
            typeName.append("Static");
        }
        if (isConst) {
            typeName.append("Const");
        }
        typeName.append(baseType);
        if (isArray()) {
            typeName.append("Array");
        }

        return typeName.toString();
    }
}
