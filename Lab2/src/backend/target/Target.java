package backend.target;

import backend.target.inst.MipsComment;
import backend.target.inst.MipsInst;
import backend.target.inst.MipsText;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class Target {
    private List<Data> dataList = new ArrayList<Data>();
    private List<MipsText> textList = new ArrayList<>();

    public void addData(Data data) {
        dataList.add(data);
    }

    public void addText(MipsText text) {
        textList.add(text);
    }

    public void dump(PrintStream out, boolean debug) {
        out.print(".data\n");
        for (var data: dataList) {
            out.print(data);
            out.print("\n");
        }
        out.println();

        out.print(".text\n");
        out.print("\tla $ra end.end\n");
        out.print("\tj main\n");

        for (var text : textList) {
            if (text instanceof MipsComment && !debug) {
                continue;
            }
            if (text instanceof MipsInst || text instanceof MipsComment)
                out.print("\t");
            out.print(text);
            out.print("\n");
        }
        out.print("end.end:");
    }
}
