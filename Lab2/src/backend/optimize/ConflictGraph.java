package backend.optimize;

import backend.ir.inst.AllocInst;

import java.util.*;

public class ConflictGraph {
    private Map<AllocInst, Set<AllocInst>> conflict = new HashMap<AllocInst, Set<AllocInst>>();

    public ConflictGraph(List<AllocInst> elements) {
        for (AllocInst element : elements) {
            conflict.put(element, new HashSet<AllocInst>());
        }
    }

    public void addConflict(AllocInst element1, AllocInst element2) {
        conflict.get(element1).add(element2);
        conflict.get(element2).add(element1);
    }

    public void removeNode(AllocInst element) {
        conflict.remove(element);
        for (AllocInst node : conflict.keySet()){
            conflict.get(node).remove(element);
        }
    }

    public ConflictGraph copy(){
        ConflictGraph newGraph = new ConflictGraph(this.conflict.keySet().stream().toList());

        for (AllocInst element : this.conflict.keySet()){
            Set<AllocInst> newSet = new HashSet<>(this.conflict.get(element));
            newGraph.conflict.put(element, newSet);
        }

        return newGraph;
    }

    public boolean isEmpty(){
        return conflict.isEmpty();
    }

    public Set<AllocInst> getConflict(AllocInst element){
        return conflict.get(element);
    }

    public Set<AllocInst> getNodes(){
        return conflict.keySet();
    }
}
