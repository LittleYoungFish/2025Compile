package backend.target.inst;

import backend.target.value.TargetValue;

import java.util.ArrayList;
import java.util.List;

public class MipsInst extends MipsText{
    private String instName;
    private List<TargetValue> values = new ArrayList<>();

    public MipsInst(String instName, TargetValue... values) {
        this.instName = instName;
        this.values.addAll(List.of(values));
    }

    @Override
    public String toString() {
        String sb = String.format("%-6s ", instName) +
                String.join(", ", values.stream().map(TargetValue::toString).toList());
        return sb;
    }
}
