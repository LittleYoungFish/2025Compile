package frontend.symtable.symbol;

import backend.ir.Value;
import frontend.symtable.SymbolTable;

public abstract class Symbol {
    public String ident;
    public SymbolTable table;
    public Value targetValue;

    public abstract String getTypeName();
}
