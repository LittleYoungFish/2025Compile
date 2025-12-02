package backend.optimize;

import backend.ir.BasicBlock;
import backend.ir.inst.AllocInst;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConflictGraphBuilder {
    private ConflictGraph graph;

    public ConflictGraph getGraph(){
        return graph;
    }

    public ConflictGraphBuilder(List<AllocInst> allocInsts, Map<BasicBlock, Set<AllocInst>> activeSets, Map<BasicBlock, Set<AllocInst>> inSets){
        Set<BasicBlock> basicBlocks = activeSets.keySet();
        graph = new ConflictGraph(allocInsts);

        for (BasicBlock block : basicBlocks){
            Set<AllocInst> defSet = activeSets.get(block);
            Set<AllocInst> inSet = inSets.get(block);

            for (AllocInst def : defSet){
                if(!allocInsts.contains(def)){
                    continue;
                }
                for (AllocInst in : inSet){
                    if (!allocInsts.contains(in)){
                        continue;
                    }
                    if (def != in){
                        graph.addConflict(def, in);
                    }
                }
            }
        }
    }
}
