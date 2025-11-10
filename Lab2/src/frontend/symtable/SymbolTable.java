package frontend.symtable;

import frontend.symtable.symbol.Symbol;

import java.io.PrintStream;
import java.util.*;

public class SymbolTable {
    private static final Set<String> LIBRARY_FUNCTIONS = new HashSet<>(Arrays.asList("getint", "printf"));

    private SymbolTable preTable = null;
    private final List<SymbolTable> nextTables = new ArrayList<SymbolTable>();
    private final Map<String, Symbol> symbolMap = new LinkedHashMap<>();

    public boolean contains(String name){
        return symbolMap.containsKey(name);
    }

    public void insertSymbol(Symbol s){
        symbolMap.put(s.ident, s);
        s.table = this;
    }

    public Symbol getSymbol(String name){
        if(symbolMap.containsKey(name)){
            return symbolMap.get(name);
        }

        if(preTable != null){
            return preTable.getSymbol(name);
        }else {
            return null;
        }
    }

    public SymbolTable createSubTable(){
        SymbolTable subTable = new SymbolTable();
        nextTables.add(subTable);
        subTable.preTable = this;
        return subTable;
    }

    public SymbolTable getPreTable(){
        return preTable;
    }

    public List<SymbolTable> getNextTables(){
        return nextTables;
    }

    public Map<String, Symbol> getSymbolMap(){
        return symbolMap;
    }

    public void printSymbolTable(PrintStream out, int depth, int[] currentScopeId){
        // 打印当前作用域的所有符号
        for(Symbol symbol : symbolMap.values()){
            if (!LIBRARY_FUNCTIONS.contains(symbol.ident)) { // 过滤 getint 和 printf
                out.println(currentScopeId[0] + " " + symbol.ident + " " + symbol.getTypeName());
            }
        }

        // 递归打印子作用域
        int childId = currentScopeId[0] + 1;
        for (SymbolTable subTable : nextTables) {
            currentScopeId[0] = childId;
            subTable.printSymbolTable(out, depth + 1, currentScopeId);
            childId = currentScopeId[0] + 1;
        }
    }
}
