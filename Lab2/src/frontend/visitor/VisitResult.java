package frontend.visitor;

import backend.ir.BasicBlock;
import backend.ir.Value;
import frontend.symtable.symbol.Type;

import java.util.ArrayList;
import java.util.List;

public class VisitResult {
    public Type expType = new Type();
    // 存储单个常量表达式的值（如3 + 5 的结果8）
    public Integer constVal;
    // 存储一组常量初始值的列表，将多个常量表达式的值按顺序收集
    public List<Integer> constInitVals = new ArrayList<Integer>();
    public List<Type> paramTypes = new ArrayList<>();

    public Value irValue;
    public List<Value> irValues = new ArrayList<>();

    public List<BasicBlock> andBlocks = new ArrayList<>();
    public List<BasicBlock> nearAndBlocks = new ArrayList<>();
    public List<BasicBlock> blocksToTrue = new ArrayList<>();
    public List<BasicBlock> blocksToFalse = new ArrayList<>();

    public boolean lvalLoadNotNeed = false;
}
