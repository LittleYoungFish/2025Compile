package frontend.visitor;

import backend.ir.BasicBlock;
import backend.ir.Function;
import backend.ir.GlobalValue;
import backend.ir.Module;
import backend.ir.inst.BrInst;
import error.ErrorRecorder;
import frontend.symtable.SymbolTable;

import java.util.List;
import java.util.Map;
import java.util.Stack;

public class SubVisitor {
    protected Visitor visitor;

    public SubVisitor(Visitor visitor) {
        this.visitor = visitor;
    }

    protected boolean isGlobalVar(){
        return visitor.isGlobalVar();
    }

    protected int isInLoop(){
        return visitor.getIsInLoop();
    }

    public boolean isRetExpNotNeed() {
        return visitor.isRetExpNotNeed();
    }

    protected SymbolTable getCurrentSymbolTable() {
        return visitor.getCurrentSymbolTable();
    }

    protected Function getCurrFunction() {
        return visitor.getCurrFunction();
    }

    protected Module getIrModule(){
        return visitor.getIrModule();
    }

    protected ErrorRecorder getErrorRecorder() {
        return visitor.getErrorRecorder();
    }

    protected BasicBlock getCurrBasicBlock(){
        return visitor.getCurrBasicBlock();
    }

    protected VarVisitor getVarVisitor(){
        return visitor.getVarVisitor();
    }

    protected DeclVisitor getDeclVisitor(){
        return visitor.getDeclVisitor();
    }

    protected FuncVisitor getFuncVisitor(){
        return visitor.getFuncVisitor();
    }

    protected ExpVisitor getExpVisitor(){
        return visitor.getExpVisitor();
    }

    protected StmtVisitor getStmtVisitor(){
        return visitor.getStmtVisitor();
    }

    public Map<String, GlobalValue> getStaticGlobalMap() {
        return visitor.getStaticGlobalMap();
    }

    public Map<String, Integer> getStaticVarCounter() {
        return visitor.getStaticVarCounter();
    }

    public Stack<List<BrInst>> getBreakBrInsts() {
        return visitor.getBreakBrInsts();
    }

    public Stack<List<BrInst>> getContinueBrInsts() {
        return visitor.getContinueBrInsts();
    }

    public void setIsGlobalVar(boolean isGlobalVar) {
        visitor.setIsGlobalVar(isGlobalVar);
    }

    public void setIsInLoop(int isInLoop) {
        visitor.setIsInLoop(isInLoop);
    }

    protected void incrementIsInLoop() {
        visitor.incrementIsInLoop();
    }

    protected void decrementIsInLoop() {
        visitor.decrementIsInLoop();
    }

    public void setRetExpNotNeed(boolean retExpNotNeed) {
        visitor.setRetExpNotNeed(retExpNotNeed);
    }

    public void setCurrentSymbolTable(SymbolTable currentSymbolTable) {
        visitor.setCurrentSymbolTable(currentSymbolTable);
    }

    public void setCurrFunction(Function currFunction) {
        visitor.setCurrFunction(currFunction);
    }

    public void setCurrBasicBlock(BasicBlock currBasicBlock) {
        visitor.setCurrBasicBlock(currBasicBlock);
    }
}
