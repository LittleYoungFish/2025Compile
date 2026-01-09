package frontend.symtable.symbol;

import java.util.ArrayList;
import java.util.List;

public class Type {
    public String type;
    public final List<Integer> dims = new ArrayList<Integer>();

    public Type(final String type) {
        this.type = type;
    }

    public Type(){
        ;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Type o) {
            return type.equals(o.type) && dims.equals(o.dims);
        }else{
            return false;
        }
    }
}
